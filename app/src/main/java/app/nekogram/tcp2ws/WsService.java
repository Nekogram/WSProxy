package app.nekogram.tcp2ws;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class WsService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int ONGOING_NOTIFICATION_ID = 6356;
    private static final String CHANNEL_WS_SERVICE = "ws_service";

    private final WsBinder binder = new WsBinder();
    private SharedPreferences preferences;
    private Tcp2WsServer tcp2ws;
    private boolean running;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        reload();
    }

    public class WsBinder extends Binder {
        WsService getService() {
            return WsService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
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
                        .setSmallIcon(R.drawable.round_settings_ethernet_24)
                        .setContentIntent(pendingIntent)
                        .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        Tcp2WsServer.setCdnDomain(preferences.getString("domain", "example.com"));
        Tcp2WsServer.setTls(preferences.getBoolean("enable_tls", true));
        Tcp2WsServer.setConnHash("");
        Tcp2WsServer.setUserAgent(preferences.getString("user_agent", "WsProxy"));
        tcp2ws = new Tcp2WsServer();
        try {
            int port = Integer.parseInt(preferences.getString("port", "42069"));
            if (port > 65535 || port < 1) {
                throw new IllegalArgumentException(getString(R.string.invalid_port_number));
            }
            tcp2ws.start(port);
            running = true;
        } catch (Exception e) {
            postError(e.getLocalizedMessage());
            stop();
        }
        return START_STICKY;
    }

    public void reload() {
        tcp2ws.stop();
        Tcp2WsServer.setCdnDomain(preferences.getString("domain", "example.com"));
        Tcp2WsServer.setTls(preferences.getBoolean("enable_tls", true));
        Tcp2WsServer.setConnHash("");
        Tcp2WsServer.setUserAgent(preferences.getString("user_agent", "WsProxy"));
        tcp2ws = new Tcp2WsServer();
        try {
            int port = Integer.parseInt(preferences.getString("port", "42069"));
            if (port > 65535 || port < 1) {
                throw new IllegalArgumentException(getString(R.string.invalid_port_number));
            }
            tcp2ws.start(port);
        } catch (Exception e) {
            postError(e.getLocalizedMessage());
            stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        tcp2ws.stop();
        stopForeground(true);
        stopSelf();
        running = false;
    }

    private void postError(String error) {
        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_WS_SERVICE)
                        .setContentTitle(getText(R.string.failed_to_start_proxy))
                        .setContentText(error)
                        .setSmallIcon(R.drawable.round_settings_ethernet_24)
                        .setColor(ContextCompat.getColor(this, R.color.ic_launcher_background))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        tcp2ws.stop();
        running = false;
        super.onDestroy();
    }
}