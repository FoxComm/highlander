# FoxCommerce Demo Storefront

[![Build status](https://badge.buildkite.com/99167bb0d9f818e7018b5bea587dceb9c7540912eda5e4669b.svg)](https://buildkite.com/foxcommerce/the-perfect-gourmet)

FoxCommerce Strorefront.js package. Isomorphic React app powered by FoxCommerce backend API.

## Prerequisites

* `node` > v5.1.0

  To install this or another versions of node you can use [brew](http://brew.sh), [n](https://github.com/tj/n) or [nvm](https://github.com/creationix/nvm)

* `yarn` > v0.20.*

* Following environment variables should be configured:
  
  - `STRIPE_PUBLISHABLE_KEY` stripe.js publishable key
  - `PHOENIX_PUBLIC_KEY` public key for JWT verification
  - `API_URL` url of FoxCommerce backend API instance
  - `GA_TRACKING_ID` if you want to enable google analytics reporting for storefront 

## Usage

```js
const FoxStorefront = require('@foxcomm/storefront.js');

const app = new FoxStorefront();
app.start();
```

or

```js
const FoxStorefront = require('@foxcomm/storefront.js');

const app = new FoxStorefront();
app.dev();
```

if you want you changes will be automatically applied while development.

## Overriding

You can override any js/jsx/css resource in the storefront if that resource is included by non-relative path.

For example you could create `src/css/colors.css` file with following content:

```css
@import '@foxcomm/storefront.js/lib/css/colors.css';

:root {
    --button-label-color: red;
}
```

in order to override label color for all buttons in the storefront.

Or you can create `src/ui/buttons.jsx` with following content:

```jsx
import React from 'react';
import classnames from 'classnames/dedupe';

import styles from '@foxcomm/storefront.js/lib/ui/css/buttons.css';
import { Button as FoxButton } from '@foxcomm/storefront.js/lib/ui/buttons';

export const Button = (props) => {
  const { className, ...rest } = props;
  return (
    <FoxButton className={classnames(className, 'my-button')} {...rest}>
      {props.children}
    </FoxButton>
  );
};

export const SecondaryButton = (props) => {
  const { className, ...rest } = props;
  return (
    <Button className={classnames(styles._secondary, className)} {...rest} />
  );
};

export default Button;
```
In order to add `my-button` class to storefront's Button component.
Basically you can override any component that being included by absolute path in the storefront,
but you should keep interface of that component.
