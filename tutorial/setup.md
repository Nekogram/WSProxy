# Set-up a domain

## Requirements

1. A domain you have full control with.
1. A [CloudFlare](https://cloudflare.com) account.

## At CloudFlare

1. Add your domain to CloudFlare.
1. Download and modify [records.txt](./records.txt), replace all `<Your domain>` with your actual domain.
1. Import DNS records from this file, make sure "Proxy imported DNS records" is checked.
1. Set SSL/TLS encryption mode in "SSL/TLS" section to Flexible.

## At APP

1. Set domain to your domain.
