FORMAT: 1A
HOST: https://developer.foxcommerce.com

# API Reference

--- row

<<< left
The FoxCommerce API is organized around [REST](
http://en.wikipedia.org/wiki/Representational_State_Transfer)
and is organized around predictable, resource-oriented URLs. All API endpoints
are stateless, respond with JSON, and indicate errors using HTTP status codes
with standard error messages.

To make this API easy to explore, this document contains test credentials that
will allow you access the public demo site at
[demo.foxcommerce.com](https://demo.foxcommerce.com).
<<<

>>> right
### API Libraries

Official libraries are available to aid in the development of storefront and
administrative JavaScript applications. For storefront applications: see
[API.js](https://github.com/FoxComm/api-js). For administrative applications:
see [AdminAPI.js](https://github.com/FoxComm/admin-api-js).

#### API Endpoint

```
https://demo.foxcommerce.com
```
>>>

---


## Authentication

--- row

<<< left
Authenticate your account using your organization, username, and password to
create a [JSON Web Token](https://jwt.io) (JWT) that can be passed to the API as
a header or cookie. The standard JWT will give you access to the whole system
full privileges to modify whatever you want - so be sure to keep the token safe!

The token that is created during login or signup is signed using public/private
key encryption, to prevent the token from being tampered with or modified by any
entity other than the Fox Platform.

The token itself contains information about the requestor's account, permissions,
and expiration time. Additionally, all requests made through the API that modify
data are recorded in the Activity Log and associated with the user identified by
the JWT.

All API requests must be made over HTTPS. Calls made over plain HTTP will fail.
Most endpoints will require the JWT to be included with the request, or else
they will fail with a `401` error. A few, however, are public and will succeed,
and some may return a JWT used to indicate a guest session.
<<<

---

--- row

<<< left
### Obtaining a Token

Request a token by logging into with your organization, email, and password.

::: note
**Traditional API tokens are coming soon!**

For now, keep using JWT made through the login endpoint. You'll experience no
loss in functionality, but will need to recreate the token daily (as it will
expire).
:::
<<<

>>> right
#### Example Request

```
$ curl -i \
    -H "Content-Type: application/json" \
    -X POST -d '{"org":<org>,"email":<email>,"password":<password>}' \
    https://demo.foxcommerce.com/api/v1/public/login
```

Replace the values `<org>`, `<email>`, and `<password>` with your organization,
email, and password, respectively.
>>>

---

--- row
<<< left

### User Permissions

In addition to identifying the user, the token is also responsible for
communicating permissions information, specifically:

- **Scope** - the ID of the organization that the user belongs to;
- **Role** - whether the user is a customer or administrator;
- **Claims** - specific, fine-grained permissions for specific resources.

#### Scope

Scope is a data primitive that is used for permissioning across the platform
while acting as a representation of organizational structure inside the Fox
Platform. It is a hierarchical structure for organizing users, and their data,
in ways that give tremendous flexibility in providing access to members of an
organization.

To illustrate this concept, consider the following example. In this example, we
have three scopes: 1, 2, and 3. 1 is the parent of 2 and 3, and they form the
following tree:

```
┌───────┐
│   1   │
│  / \  │
│ 2   3 │
└───────┘
```

Scopes are written in the token as a '.' delimited string. So, in the token, the
scopes would appear as:

- 1 = 1
- 2 = 1.2
- 3 = 1.3

Because of the parent-child nature of the relationship, scope `1` has access to
all data under `1`, `1.2`, and `1.3`, while `1.2`, `1.3` only have access to
their own data.

#### Role

Role describes a collection of permissions assigned to a user. There are
currently two roles that a user may have:

- admin
- customer

#### Claim

Claims are fine-grained permissions on specific resources in the system. A claim
is a combination of three pieces:

- Fox Resource Name (FRN): A string that identifies a resource type, such as a
  product or order;
  - `frn:pim:sku` = FRN for a SKU
  - `frn:oms:order` = FRN for an order
- Scope: A '.' delimited scope, as shown above;
- Actions: An array of strings, signifying the basic actions.
  - `c` = create
  - `r` = read
  - `u` = update
  - `d` = delete

A claim is written as: `"<FRN>:<Scope>": [<actions>]`

<<<
>>> right

```
{
  "aud": "user",
  "id": 1,
  "email": "admin@admin.com",
  "ratchet": 0,
  "scope": "1",
  "roles": ["admin"],
  "name": "Frankly Admin",
  "claims": {
    "frn:usr:org:1": ["c", "r", "u", "d"],
    "frn:pim:sku:1": ["c", "r", "u", "d"],
    "frn:oms:cart:1": ["c", "r", "u", "d"],
    "frn:usr:user:1": ["c", "r", "u", "d"],
    "frn:oms:order:1": ["c", "r", "u", "d"],
    "frn:pim:album:1": ["c", "r", "u", "d"],
    "frn:pim:coupon:1": ["c", "r", "u", "d"],
    "frn:pim:product:1": ["c", "r", "u", "d"]
  },
  "exp": 1525895101,
  "iss": "FC"
}
```
>>>

---

## Errors

--- row

<<< left
The Fox Platform uses conventional HTTP response codes to indicate the success
or failure of an API request. In general, codes in the `2xx` range indicate
success, codes in the `4xx` range indicate an error that failed given the
information provided (e.g., a required parameter was omitted, etc.), and codes
in the `5xx` range indicate an error with Fox's platform.

### Error Object

| Field | Description |
|-------|-------------|
| errors | An array of strings that provide information about the error that occurred |
<<<

>>> right
### HTTP Status Code Summary

| Code               | Meaning                                                                |
|--------------------|------------------------------------------------------------------------|
| 200 - OK           | Everything worked as expected                                          |
| 201 - Created      | Entity was successfully created                                        |
| 400 - Bad Request  | The request was unacceptable, often due to missing required parameters |
| 401 - Unauthorized | JWT was missing or user didn't have permission to perform API request  |
| 404 - Not Found    | The requested resource doesn't exist                                   |
| 500 - Server Error | Something with wrong with the Fox Platform                             |
>>>
---

<!-- include(objects/activity_trail.apib) -->
<!-- include(objects/common.apib) -->
<!-- include(objects/coupons.apib) -->
<!-- include(objects/credit_cards.apib) -->
<!-- include(objects/customers.apib) -->
<!-- include(objects/customers_groups.apib) -->
<!-- include(objects/gift_card.apib) -->
<!-- include(objects/location.apib) -->
<!-- include(objects/notifications.apib) -->
<!-- include(objects/notes.apib) -->
<!-- include(objects/order.apib) -->
<!-- include(objects/promotions.apib) -->
<!-- include(objects/reasons.apib) -->
<!-- include(objects/reviews.apib) -->
<!-- include(objects/returns.apib) -->
<!-- include(objects/save_for_later.apib) -->
<!-- include(objects/shared_search.apib) -->
<!-- include(objects/store_credit.apib) -->
<!-- include(objects/store_admin.apib) -->
<!-- include(objects/album.apib) -->
<!-- include(objects/object.apib) -->
<!-- include(objects/variant.apib) -->
<!-- include(objects/taxonomy.apib) -->
<!-- include(objects/stock_location.apib) -->
<!-- include(objects/stock_item.apib) -->
<!-- include(objects/shipment.apib) -->
<!-- include(objects/export.apib) -->

<!-- include(public.apib) -->
<!-- include(customers.apib) -->
<!-- include(products.apib) -->
<!-- include(merchandising.apib) -->
<!-- include(inventory.apib) -->
<!-- include(transactions.apib) -->
<!-- include(discounts.apib) -->
