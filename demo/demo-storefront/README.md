# A Minimal Storefront
#### Built on the Fox Commerce platform

The minimum requirement for a storefront is:

```
config.yml
Dockerfile
index.js
package.json
/css
/templates
/static
```

### Development

Run:

```
$ yarn
$ npm run dev
```


## Our Opinions about Frontend Build
You should use `webpack`. It’s more powerful and does more stuff with less code.


## Our Opinions about `css`

#### Embrace the cascade
A lot has been said lately of the power of encapsulation to avoid side-effects.
This can be true of a large development team who doesn't communicate very well
about standards or implementation. But for most situations under 10 developers,
a clearly-defined styleguide can use powerful core styles to maintain consistency.

The downside of encapsulation is, not DRY! Remember DRY? It was awesome. If you
have deep encapsulation, you may as well be using `<font>` tags.

#### Layout
Grids are bad. Simple, human-readable division of space is good. Using 7 of 12
columns is ugly and [YAGNI](https://en.wikipedia.org/wiki/You_aren%27t_gonna_need_it);
dividing visual sections into halves, thirds and quarters is how both designers
and website users think 99% of the time. So that's what our mini css layout
"framework" does. Using `flexbox`, with a few simple alignment modifiers.

#### Typography
Should be simple, core defined. Less is more. Set number of defined title hierarchy.
