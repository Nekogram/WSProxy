package app.nekogram.tcp2ws;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.tcp2ws.tcp2wsServer;

public class WsService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int ONGOING_NOTIFICATION_ID = 6356;
    private static final String CHANNEL_WS_SERVICE = "ws_service";

    private SharedPreferences preferences;
    private tcp2wsServer tcp2ws;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        reload();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        @SuppressLint("InlinedApi") PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(CHANNEL_WS_SERVICE, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getText(R.string.channel_name))
                .setVibrationEnabled(false)
                .setLightsEnabled(false)
                .build();

        NotificationManagerCompat.from(this).createNotificationChannel(channel);

        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_WS_SERVICE)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.baseline_settings_ethernet_24)
                        .setContentIntent(pendingIntent)
                        .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        tcp2wsServer.setCdnDomain(preferences.getString("domain", "example.com"));
        tcp2ws = new tcp2wsServer()
                .setTls(preferences.getBoolean("enable_tls", true))
                .setConnHash("")
                .setUserAgent(preferences.getString("user_agent", "WsProxy"));
        try {
            int port = Integer.parseInt(preferences.getString("port", "42069"));
            if (port > 65535 || port < 1) {
                throw new IllegalArgumentException(getString(R.string.invalid_port_number));
            }
            tcp2ws.start(port);
        } catch (Exception e) {
            postError(e.getLocalizedMessage());
            stopSelf();
        }
        return START_STICKY;
    }

    public void reload() {
        tcp2ws.stop();
        tcp2wsServer.setCdnDomain(preferences.getString("domain", "example.com"));
        tcp2ws = new tcp2wsServer().setTls(preferences.getBoolean("enable_tls", true))
                .setConnHash("")
                .setUserAgent(preferences.getString("user_agent", "WsProxy"));
        Log.e("Test", preferences.getString("port", "42069"));
        try {
            int port = Integer.parseInt(preferences.getString("port", "42069"));
            if (port > 65535 || port < 1) {
                throw new IllegalArgumentException(getString(R.string.invalid_port_number));
            }
            tcp2ws.start(port);
        } catch (Exception e) {
            postError(e.getLocalizedMessage());
            stopSelf();
        }
    }

    public void postError(String error) {
        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_WS_SERVICE)
                        .setContentTitle(getText(R.string.failed_to_start_proxy))
                        .setContentText(error)
                        .setSmallIcon(R.drawable.baseline_settings_ethernet_24)
                        .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        tcp2ws.stop();
        super.onDestroy();
    }
}