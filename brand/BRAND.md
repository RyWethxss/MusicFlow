# WETHWORKS — Identity system 1.0

**Built to be understood.**

WETHWORKS is an umbrella identity for interactive software and experiments. The first product in this system is **Parking Lab**. A product can change its subject and capabilities; its visual parent stays recognizable. The brand is independent of the earlier APEX product.

## The face

The open W is a continuous, forward-moving line with two grounded points. Pair it with the outlined WETHWORKS wordmark. The mark is original geometry; the wordmark is outlined from the bundled Manrope font, so its SVG rendering has no external font dependency.

Use `logos/wordmark-dark.svg` on light surfaces, `wordmark-light.svg` on dark surfaces, and the symbol alone only where a full wordmark cannot fit. Keep the symbol upright and preserve the SVG proportions. Clear space is at least half the logo's height. Recommended minimum wordmark width is 168 px; a standalone symbol should be at least 24 px high. Do not add ® or invent registration claims.

Product lockup: **WETHWORKS / Parking Lab**, **WETHWORKS / [Product name]**. The umbrella wordmark remains the same. Do not create a different parent logo for each project.

## Color and surface

| Role | Token | Value | Use |
| --- | --- | --- | --- |
| Canvas | `--ww-paper` | `#F4F1E9` | Warm base for the surrounding application. |
| Surface | `--ww-surface` | `#FFFDF7` | Controls, reading areas, quiet panels. |
| Ink | `--ww-ink` | `#17221D` | Main text and strong graphic fields. |
| Secondary text | `--ww-muted` | `#5B685D` | Secondary labels with readable contrast. |
| Accent | `--ww-accent` | `#C44723` | Primary controls, selected states, restrained brand marks. |
| Accent text | `--ww-accent-ink` | `#A93A1C` | Small links or orange text on the paper base. |
| Sage | `--ww-sage` | `#B7C8A1` | Quiet positive fills, large signals, scenery. |
| Success text | `--ww-success` | `#43613B` | Success text and validation labels. |
| World | `--ww-stage` | `#29382E` | Deep scenes and camera frames. |
| Prediction | `--ww-prediction` | `#80AEB7` | Predicted/observed comparison series, with textual labels. |

The primary surface pair is warm paper plus graphite. Orange is a deliberate action color, not a wash over the whole page. Reserve dark sections for the simulation, camera views or focused technical inspection. A realistic 3D scene keeps its own physically plausible materials; do not recolor roads or car interiors orange to imitate the UI.

Measured static sRGB contrast ratios: ink/paper 14.50:1, muted/paper 5.19:1, success/paper 6.18:1, surface/accent 4.83:1. Accent on paper is 4.35:1, so use accent-ink for small orange text. These are token checks, not a WCAG certification for a complete product.

## Type

Use the bundled **Manrope** variable font for headings, controls and prose. Use **IBM Plex Mono Regular** for numeric telemetry, seeds, units, compact identifiers and technical captions. Load `tokens.css`; keep its `fonts/` directory beside it. The font licenses are included. Do not substitute a different display font per product.

- Main product heading: 36–72 px, weight 550–650, tight but readable tracking.
- Section heading: 19–24 px, weight 650.
- Body and important controls: 13–15 px, comfortable line height.
- Telemetry: 13–20 px mono, tabular numerals, explicit units.
- Supporting technical labels: 11 px minimum in the reusable system. Do not hide essential information in tiny microtext.
- Uppercase belongs in short mono labels. Avoid entire paragraphs in uppercase or mono.

## Shared application structure

1. A quiet masthead: WETHWORKS lockup, product title and at most a few genuine navigation/actions.
2. A direct statement of the current task and its state. Avoid oversized marketing copy competing with an active simulation.
3. The primary working surface, with the main scene or document taking the largest area.
4. Settings and inspection in purposeful rails or drawers. Use progressive disclosure for advanced settings.
5. Transport/mode controls with clear control ownership. A camera, graph or animation must be based on real state and labelled by what it actually represents.

Use a 4 px spacing grid, mostly 8/16/24/32 px increments. Main desktop gutters are 24–32 px. Corners are restrained: 4 px controls, 8 px panels, 12 px only for larger surfaces. Use hairline borders and a quiet shadow; avoid pill-shaped controls everywhere.

## Interaction and state

The main action uses orange with warm-white text. Secondary actions use light surfaces and borders. Destructive actions use the error color and explicit words. A mode toggle states who has control. Disabled controls look disabled and explain why when it matters.

A color never carries the entire meaning: pair success/error/prediction states with words, icons or numbers. Keyboard focus is visible. Target 42–44 px for primary interactive controls; support reduced motion and explicit pause for continuous activity. A product-specific dark theme must use the same typography, geometry and naming.

## Voice

Product copy is clear English for international release. Speak directly and explain consequences: “Reset car — keep learned weights”, “Student prediction”, “Human control”. Use real units and truthful algorithm names. No fake percentages, fabricated success claims, decorative telemetry or unsupported “production autonomy” language. Direct collaboration remains in Turkish.

## Keep consistent

Future projects should copy this directory as `brand/`, import `brand/tokens.css`, use the canonical logos and link this document in their project instructions. They can add scoped product tokens but should not redefine the core identity values. Record intentional identity changes in this document and increment its version. Domain-specific illustrations, 3D scenes and workflows can be original without changing the parent identity.
