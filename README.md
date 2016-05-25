# FoxCommerce API.js

## Overview

API.js is a JavaScript library that helps FoxCommerce customers create powerful, customized
storefronts on top of the FoxCommerce API. It does this by creating an easy abstraction of available API
operations and by providing convenient helper methods.

This API layer is framework agnostic as we don't to impose a framework decision such as React or Angular
on customers.

API.js will be build in ES6 and transpiled to ES5. It won't use ES7 features.


### Spec

Principles:

- The library should have simple method names that match core API methods, or simplifications or sugar on the core API.
- It should make instantiation/authentication as simple as possible.
- It should deal with Async, probably a Fetch/Promise-based model.
- As a rule of thumb, `.then()` should be left to the implementation of the library [ie. not in this codebase].
  Possible exception here is Auth section, storage of `jwt` tokens, handling oAuth redirects etc.

# Usage

```
import FoxCommAPI from `FoxComm/api-js`
const FxC = new FoxCommAPI({ args })  // args could include API domain & path data, public_key, etc
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


# Product catalog

In Process...

```
FxC.getProduct(id | slug)
    → { product }

FxC.getProducts(null | filters/categories/etc)
    → [{ products }]

FxC.search(query)
    → [{ products }]
```

### `getProducts` Filters/Queries

The idea of this function is to be as flexible and forgiving as possible to return the most useful set of products. There are different levels of sugar:

* full, complex query object
* simplified query object [auto-detect fields]
* simple query string

Heavily inspired by WordPress’ [Taxonomy querying](https://codex.wordpress.org/Class_Reference/WP_Query#Taxonomy_Parameters).

#### Arguments

- `condition (array|object)` - Required, however if no `relation` is given, both `relation` and `condition` can be implied by passing an array of conditions or a single object at the root of the query [see examples below].
  - `taxon|optionType (string)` - Required. Taxonomy or OptionType name.
  - `value (string|array)` - Required. Taxonomy term(s) to filter by.
  - `field (string)` - Optional. Field to search taxonomy term by. Possible values: `name`, `slug` or any defined custom property. Default: `slug`.
  - `include_children (bool)` - Optional. Whether or not to include children for hierarchical taxonomies. Default: `true`.
  - `operator (string)` - Optional. Operator to test. Possible values are `IN`, `NOT IN`, `AND`, `EXISTS` and `NOT EXISTS`. Default: `IN`.
- `relation (string)` - Optional [only needed with multiple `conditions`]. Possible values: `AND`, `OR`. Default: `AND`.

All the below data can be passed to `getProducts` and will return the same result:


```
{
  taxon: 'gender',
  value: 'men'
}
-------
{
  gender: 'men'
}
-------
'gender=men'
```

Multiple filters are considered `AND` by default [i.e. `select * from products where {condition} AND {condition}`].

```
{
  relation: 'AND',
  conditions: [
    {
      taxon: 'gender',
      value: 'men'
    },
    {
      optionType: 'color',
      value: 'green'
    }
  ]
}
-------
[
  {
    gender: 'men'
  },
  {
    color: 'green'
  }
]
-------
'gender=men&color=green'
```


# Cart

```
FxC.getCart()
    → { meta, [items]}

FxC.addToCart({item} | [{items}])
    → { cart } or success | err

FxC.removeFromCart({item} | [{items}])
    → { cart } or success | err

FxC.emptyCart()
    → { cart } or success | err

```


# Checkout

```
FxC.checkout.getShippingMethods()
    → [{ methods }] | err [eg. shipping address not set]

FxC.checkout.setAddressData(address)
    → ?

FxC.checkout.setBillingData(data)
    → ?

FxC.checkout.addCreditCard(data)
    → ?

FxC.checkout.applyGiftCard(code)
    → success | err

FxC.checkout.applyPromoCode(code)
    → success | err

FxC.checkout.getCountries()
    → [{ countries }]

FxC.checkout.getStates(country)
    → [{ states }]

FxC.checkout.getCityFromZip(zip)
    → [{ city, state }]

FxC.checkout.reset()
    → success | err

FxC.checkout.finish()
    → success | err

```
