# Changelog

## v0.2 - 3 (TODO)

- Restrict the main screen's navigation drawer to 2/3 of the screen width on narrow screens. (It was always supposed to behave like this, but I had accidentally broken it shortly before the v0.1 release.)

- Handle the back button/gesture correctly when the main screen navigation drawer is open. This now closes the drawer, previously it ignored the drawer and navigated back from the main screen, typically leaving the app.

- Use the more intuitive half-up rounding mode ($0.045 rounds to $0.05) when displaying currency amounts, instead of the default half-even rounding mode ($0.045 rounds to $0.04).

- Show a unit price on the add/edit price screen, along with the current unit price and a percentage change when editing an existing price.

- Try to improve the automatic selection of unit price denominators. A new algorithm is used which may avoid poor choices in some corner cases.

- Persist a manually selected unit price denominator until a different product is selected.

## v0.1.1 - 2 (2025-12-20)

- Update gradle-wrapper to 8.13 and add a distributionSha256Sum to gradle-wrapper.properties.

## v0.1 - 1 (2025-12-18)

- First public release
