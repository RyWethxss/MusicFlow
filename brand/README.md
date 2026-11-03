# WETHWORKS brand kit

Reusable identity assets for Parking Lab and later WETHWORKS projects. Open `preview.html` for the visual specimen; read `BRAND.md` for the rules.

Copy this whole directory into a project as `brand/`, then use:

```html
<link rel="stylesheet" href="./brand/tokens.css">
<img src="./brand/logos/wordmark-dark.svg" alt="WETHWORKS" width="200">
```

The seven logos are self-contained SVG. Fonts are local; no CDN, account or API is needed. `tokens.css` supplies core variables and a small button/panel specimen, not a full application framework.

## Font provenance

- Manrope variable font: [official Google Fonts directory](https://github.com/google/fonts/tree/main/ofl/manrope), bundled SIL Open Font License in `fonts/Manrope-OFL.txt`.
- IBM Plex Mono Regular: [official Google Fonts directory](https://github.com/google/fonts/tree/main/ofl/ibmplexmono), bundled SIL Open Font License in `fonts/IBMPlexMono-OFL.txt`.

Fonts were downloaded from the official Google Fonts source repository on 17 October 2026. The outlined wordmark was generated from Manrope at weight 760. The W symbol is original vector geometry. FontTools was used only as a temporary build utility; it is not a runtime dependency.
