# Changelog

## v0.2 - 3 (TODO)

- Restrict the main screen's navigation drawer to 2/3 of the screen width on narrow screens. (It was always supposed to behave like this, but I had accidentally broken it shortly before the v0.1 release.)

- Handle the back button/gesture correctly when the main screen navigation drawer is open. This now closes the drawer, previously it ignored the drawer and navigated back from the main screen, typically leaving the app.

## v0.1.1 - 2 (2025-12-20)

- Update gradle-wrapper to 8.13 and add a distributionSha256Sum to gradle-wrapper.properties.

## v0.1 - 1 (2025-12-18)

- First public release
