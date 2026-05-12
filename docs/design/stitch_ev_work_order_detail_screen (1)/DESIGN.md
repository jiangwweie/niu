---
name: Technical Service WeUI System
colors:
  surface: '#f3fcf0'
  surface-dim: '#d4ddd1'
  surface-bright: '#f3fcf0'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#edf6ea'
  surface-container: '#e8f0e4'
  surface-container-high: '#e2ebdf'
  surface-container-highest: '#dce5d9'
  on-surface: '#161d16'
  on-surface-variant: '#3d4a3d'
  inverse-surface: '#2a322b'
  inverse-on-surface: '#eaf3e7'
  outline: '#6c7b6c'
  outline-variant: '#bbcbba'
  surface-tint: '#006d33'
  primary: '#006d33'
  on-primary: '#ffffff'
  primary-container: '#07c160'
  on-primary-container: '#00471f'
  inverse-primary: '#45e17c'
  secondary: '#006495'
  on-secondary: '#ffffff'
  secondary-container: '#05acfd'
  on-secondary-container: '#003d5d'
  tertiary: '#a23d33'
  on-tertiary: '#ffffff'
  tertiary-container: '#ff8475'
  on-tertiary-container: '#741c16'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#66ff95'
  primary-fixed-dim: '#45e17c'
  on-primary-fixed: '#00210b'
  on-primary-fixed-variant: '#005225'
  secondary-fixed: '#cbe6ff'
  secondary-fixed-dim: '#90cdff'
  on-secondary-fixed: '#001e31'
  on-secondary-fixed-variant: '#004b72'
  tertiary-fixed: '#ffdad5'
  tertiary-fixed-dim: '#ffb4aa'
  on-tertiary-fixed: '#410001'
  on-tertiary-fixed-variant: '#82261e'
  background: '#f3fcf0'
  on-background: '#161d16'
  surface-variant: '#dce5d9'
typography:
  nav-title:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '600'
    lineHeight: 24px
  cell-title:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 24px
  cell-value:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 24px
  body-main:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-bold:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  label-desc:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  button-text:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '600'
    lineHeight: 24px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  edge-margin: 16px
  cell-padding-v: 16px
  cell-padding-h: 16px
  card-gap: 12px
  section-gap: 24px
  safe-area-bottom: 34px
---

## Brand & Style

This design system adheres strictly to the WeChat WeUI design language, optimized for a technical service environment. The aesthetic is rooted in **Modern Minimalism**, prioritizing utility, speed, and clarity. The interface is designed to feel native to the WeChat ecosystem, reducing cognitive load for users familiar with the platform.

The brand personality is professional, dependable, and unobtrusive. It avoids decorative flourishes in favor of structured information hierarchy. By utilizing the official WeUI specifications, the system ensures high performance and accessibility in a utility-first context.

## Colors

The palette is anchored by **WeUI Green (#07C160)**, used exclusively for primary actions and affirmative states. The background uses the standard WeUI off-white to provide a soft contrast against the pure white component cards.

- **Primary:** WeUI Green for buttons, active icons, and links.
- **Background:** A neutral light gray (#F7F7F7) to define the application surface.
- **Surface:** Pure white (#FFFFFF) for interactive cards and list cells.
- **Functional Grays:** Used for borders (#E5E5E5), secondary text (#888888), and disabled states.
- **Accents:** Occasional use of WeUI Blue for informational links or secondary technical indicators.

## Typography

The design system utilizes **Inter** (as the closest high-quality match to a standard system sans-serif stack) to maintain a clean, technical look. In accordance with WeUI principles, the hierarchy is established primarily through **font-weight** rather than varying font sizes.

The base size for most interactions is 17px, which is the standard for mobile readability in technical applications. Contrast is managed by shifting from black (#000000 at 90% opacity) for primary content to a mid-gray (#888888) for descriptions and secondary labels.

## Layout & Spacing

The layout follows a **Fluid Grid** model with standardized edge margins. Content is organized into clear vertical sections to facilitate scanning in a service-oriented environment.

- **Margins:** A consistent 16px horizontal margin is applied to all main containers.
- **Cells:** The "Cell" is the foundational layout unit, featuring 16px internal padding on all sides.
- **Vertical Rhythm:** A 12px gap is maintained between individual cards, while larger 24px gaps separate distinct logical sections.
- **Fixed Elements:** The action bar is pinned to the bottom of the viewport, incorporating a safe-area-inset for modern notched devices.

## Elevation & Depth

This design system eschews traditional shadows in favor of **Tonal Layers** and **Low-Contrast Outlines**. 

Depth is communicated through the contrast between the #F7F7F7 background and the #FFFFFF cards. To define boundaries without adding visual noise, a 0.5px or 1px hairline border (#E5E5E5) is used on cards and cell separators. This creates a "flat-but-layered" effect that feels precise and technical. Shadows are only used for transient elements like pickers or action sheets, where a very soft, diffused blur (0px 2px 10px rgba(0,0,0,0.05)) may be applied.

## Shapes

The shape language is strictly controlled to maintain a professional, systematic feel. 

- **Cards & Primary Containers:** Use an 8px (0.5rem) border radius to soften the technical layout while maintaining a structured appearance.
- **Buttons:** Follow the same 8px radius for consistency.
- **Inputs:** Form fields and chips utilize the 8px radius.
- **Small Elements:** Badges and small tags may use a fully rounded (pill) shape to distinguish them from interactive containers.

## Components

### Buttons
Primary buttons use the WeUI Green (#07C160) with white text. Secondary buttons use a light gray background (#F2F2F2) with black text. Buttons are full-width when placed in the bottom action bar.

### Cells & Lists
The core navigation element. A white card containing multiple rows separated by #E5E5E5 hairlines. Each row includes a title on the left and an optional value or chevron on the right.

### Cards
Individual modules for technical data or service summaries. Cards should have an 8px radius and a white background. Padding inside cards must be a uniform 16px.

### Form Inputs
Standard WeUI style with a label on the left and the input field on the right. Validation states should be indicated by a subtle color shift in the label or a small icon, rather than heavy red borders.

### Bottom Action Bar
A fixed container at the bottom of the screen. It features a blur effect or a solid white background with a top border and houses the primary call-to-action button, ensuring critical technical steps are always accessible.

### Chips & Tags
Used for status indicators (e.g., "Pending," "Completed"). These should have low-saturation background tints of the status color with high-contrast text.