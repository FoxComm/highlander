We are using limited set of text colors, background colors and fonts. There are very few exceptions (socials, charts), but it is hightly recommended to use only existing variables for `color`, `background-color` and `font` css properties. If mockups didnt match them, discuss that with designer.

To use variables, just import them to your css file:

```css
@import 'colors.css';

.block {
  color: #123456; /* prohibited! */

  color: var(--color-text); /* allowed */
  background-color: var(--bg-grey);
}

.label {
  color: var(--color-additional-text);
  background: var(--bg-white);
  border: 1px solid var(--color-border);
}

.clickableText {
  color: var(--color-action-text);
}
```
