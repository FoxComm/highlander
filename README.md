# FoxCommerce API.js

## Overview

API.js is a JavaScript library that helps FoxCommerce customers create powerful, customized
storefronts on top of the FoxCommerce API. It does this by creating an easy abstraction of available API
operations and by providing convenient helper methods.

This API layer is framework agnostic as we don't to impose a framework decision such as React or Angular
on customers.

API.js will be build in ES6 and transpiled to ES5. It won't use ES7 features.

### WIP: Discussion on the Javascript client library for interacting with the FoxComm API

This is pseudocode / spec to come to an agreement on what a pure javascript API client wrapper library's API should look like. In describing each method I'm aiming for the minimum human-readable info: method name, arguments accepted, return value. Going for brevity here, aiming to distill the API signature maximizing developer conceptual simplicity.

I've divided into logical sections, maybe these sections can be namespaced within the library itself, as I've done with `checkout` methods.

The library should have simple method names that match core API methods.

It should make instantiation/authentication as simple as possible.

It should deal with Async, probably a Fetch/Promise-based model.

It should be designed to be used as follows:

```
import FoxCommAPI from FoxComm/api-client-js
const FxC = new FoxCommAPI()
```


# Auth

```
FxC.signup(credentials)
    → jwt | err

FxC.login(credentials)
    → jwt | err

FxC.loginWith(authProvider, credentials)
    → jwt | err

FxC.logout()
    → success | err
```

Q: should auth methods return JWT, or also deal with storing and retrieving jwt from local storage? I'm tempted to say it basically returns true/false and does all `localStorage` work for you.

Any below methods requiring login should therefore return an err if we try to use them before running auth functions.


# Product catalog

```
FxC.getProduct(id | slug)
    → { product }

FxC.getProducts(null | filters/categories/etc)
    → [{ products }]

FxC.search(query)
    → [{ products }]
```

Q: We could do a lot of different things here, from keeping everything in one function that accepts complex filtering/searching on the one hand, to breaking out different methods for `getProductsByCategory`, `getProductsByTag`, `getProductsByAttributeFilter` etc etc. Also not sure `getProducts` and `search` are different.


# Cart

```
FxC.getCart()
    → { meta, [items]}

FxC.addToCart({item} | [{items}])
    → { cart }
    → success | err

FxC.removeFromCart({item} | [{items}])
    → { cart }
    → success | err

FxC.emptyCart()
    → { cart }
    → success | err

```

Q: do cart update functions just return true/false, item, or the whole cart object again? I'm tempted to return the whole cart again so that we don't have to make another trip to the server to update cart.


# Checkout

```
FxC.checkout.shippingMethods()
    → [{ methods }] | [] err_no_country_set

FxC.checkout.setAddressData()
    → ?

FxC.checkout.setBillingData()
    → ?

FxC.checkout.reset()
    → success | err

FxC.checkout.getCountries()
    → [{ countries }]

FxC.checkout.getStates(country)
    → [{ states }]

FxC.checkout.getCityFromZip(zip)
    → [{ city, state }]

```
