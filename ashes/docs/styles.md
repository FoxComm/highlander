## Less

> Deprecated.

Less was used before css-modules were introduces. All existing components should be rewritten with css-modules. 
Less classes should be used just in case it's not yet rewritten and less class implements styles you need.

## [Css-modules](https://github.com/css-modules/css-modules)

We are using `css-modules` as a loader for `css` files. That means you could use any classnames inside one css file, because they are dynamic and always will be uniq in the output file.

Say you have `path/to/file.css` with following styles:

```css
.block {
  font-size: 20px;
}
```

In development mode all (but :global) class selectors will be extended with prefix `path⁄to⁄file__`, so the output will be like that:

```css
.path⁄to⁄file__block {
  font-size: 20px;
}
```

In production mode the transformation is more complex: it uses hash-based function to minify selector's length (and output _css file_'s size).

We are also using some postcss plugins such as 
[`postcss-nested`](https://github.com/postcss/postcss-nested) or [`postcss-cssnext`](https://github.com/MoOx/postcss-cssnext)
 — because of that our css syntax is not exactly css: it is extended.

### HTML

To insert dynamic classes to components, just use css as js objects:

```jsx
// We are inside `bar.jsx` component

import s from './bar.css'; // Importing css styles as a JS Object

...

  <div className={s.block}>...</div>
```

Use [`classnames`](https://github.com/JedWatson/classnames) lib if you need multiple classes on the same html node:

```jsx
import classNames from 'classnames';

...
  <div className={classNames(s.block, { [s._big]: this.props.isBig })}>...</div>
```

### Context-free

Each component is a `block` in `BEM` terminology: it is trying to occupy 100% width of parent and 100% height of its children content (not its parent) (there are some exceptions though, e.g. Buttons).

**All styles, which affects component's look, must be inside that component**.

If you need to change component's look depending on its parent, you can do that in two ways:

#### Modifier

You can pass boolean/string flags to the component to change its look:

```jsx
<ChildComponent isBig={true} look="awesome" />
```

The trick is that `ChildComponent` contains all the styles, including those which related to `isBig` and `look` props, inside itself:

```css
.block {
  font-size: 20px;

  &._big {
    font-size: 40px;
  }

  &._awesome {
    font-family: 'Comic Sans';
  }
}
```

In the `jsx` file you just need to add corresponding classNames:

```jsx
<div className={classNames(s.block, { [s._big]: this.props.isBig }, s[`_${this.props.look}`] })}>...</div>
```

#### className as a prop

You also could just send `className` as a prop:

```jsx
<ChildComponent className={s.classNameForChild} />
```

and add it inside child component:

```jsx
<div className={classNames(s.block, this.props.className)}>...</div>
```

But you must be sure, that you only changing styles, which is _"outside"_ the component: such as `margin`, `position`, `display`. Dont send to child component styles, which affects its look inside the `block`.

### Grouping styles around element

So, all styles of a component must be inside that component. By analogy, all element's styles sould be inside that element's ruleset.

You can use `&` to group all element's styles in one place. For example:

```css
.block {
  /* styles here */
}

/* All styles, related to `.icon`, are inside `.icon` ruleset */
.icon {
  color: red;

  .block._big & {
    color: green;
  }

  .block._awesome & {
    color: black;

    &::before {
      /* styles for `.block._awesome .icon::before` selector here */
    }
  }
}
```

### :global

If you need to use global classnames in selectors, which should not be transformed by `css-modules`, just use `:global()`:

```css
.header :global(.fc-legacy-classname) {
  font-size: 20px;

  & :global(.fc-form-field) {
    margin-bottom: 10px;
  }
}
```

Please, try to avoid `:global()` usage.
