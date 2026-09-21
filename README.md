# Tikit Masta Android v1.1.1

Native Android client for the `bz_eventpass` Odoo 18 Community module and its API v1.7.

## Included

- Public event list and web ticket checkout
- Dynamic event cards with artwork and live availability
- Event details with ticket types and pricing
- QR ticket scanner with accepted/rejected result
- Scanner device status authentication
- Gate sales form
- Promoter dashboard
- Configurable Odoo URL and scanner device token
- Professional customer/staff dashboard and improved loading/error states
- Explicit accessible form labels, text colors, and navigation-bar spacing
- Android 7.0+ support

## First use

1. Install the APK and open **Settings**.
2. Enter the full HTTPS Odoo address, such as `https://events.example.com`.
3. For staff features, paste the scanner device token created in Odoo.
4. Tap **Save & Test**.

Customer event browsing requires no device token. Ticket scanning and gate sales require an active scanner device token. The Odoo module must be installed and API health should respond at `/bz_eventpass/api/v1/health`.

## Build

Open the project in Android Studio Ladybug or later and build the `release` variant. The current release configuration uses the Android debug signing key for direct testing; configure your private upload key before Play Store distribution.
