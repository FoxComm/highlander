# FoxCommerce API.js

## Overview

API.js is a JavaScript library that helps FoxCommerce customers create powerful, customized
storefronts on top of the FoxCommerce API. It does this by creating an easy abstraction of available API
operations and by providing convenient helper methods.

This API layer is framework agnostic as we don't to impose a framework decision such as React or Angular
on customers.

API.js will be build in ES6 and transpiled to ES5. It won't use ES7 features.

[API documentation](http://foxcomm.github.io/api-js/)

### Spec

Principles:

- The library should have simple method names that match core API methods, or simplifications or sugar on the core API.
- It should make instantiation/authentication as simple as possible.
- It should deal with Async, probably a Fetch/Promise-based model.
- As a rule of thumb, `.then()` should be left to the implementation of the library [ie. not in this codebase].
  Possible exception here is Auth section, storage of `jwt` tokens, handling oAuth redirects etc.

### debugging

You can log requests via settings [DEBUG](https://www.npmjs.com/package/debug) env variable to `foxapi`:

```
DEBUG=foxapi npm test
```

# Usage

```
import FoxCommAPI from `FoxComm/api-js`
const FxC = new FoxCommAPI({ args })  // args could include API domain & path data, public_key, etc
```

[working draft](./working-draft.md)
