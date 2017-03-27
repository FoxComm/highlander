
1. `POST /v1/my/cart/line-items` & `PATCH /v1/carts/${referenceNumber}/line-items` - `response.lineItems.skus.totalPrice` doesn't take item quantity into calculation (assumes item quantity is always 1), but successive `GET /v1/my/cart` works correctly.
2. It is possible create order without a shipping method, but impossible to open its details page.
3. Order `fraudScore` inconsistency: `POST /v1/my/cart/checkout` -> fraudScore = 0, `GET /v1/orders/${referenceNumber}` -> fraudScore = 7 (for example).
4. `PATCH /v1/gift-cards/${giftCardCode}` response has no `updatedAt` field and no `storeAdmin` field (but it is present in response of `POST /v1/gift-cards`).
5. `PATCH /v1/albums/${context}/${albumId}` & `POST /v1/albums/${context}/${albumId}/images` don't update `updatedAt` field of album.
6. `DELETE /v1/carts/${referenceNumber}/watchers/${watcherId}` returns nothing, should it return new list of watchers?
7. `DELETE /customers/:cid/addresses/:aid` returns nothing, should it return new list of addresses?
8. `POST /v1/credit-card-token (CreditCardDetailsPayload)` - cvv is integer and not handled properly (1-2 digit numbers not treated as having leading zeros).
9. `PATCH /v1/products/${context}/${productId}` - slug is not being updated after changing `attributes.title` - ok?

----------------------------------------------------------------------------------

api.post api.get api.patch api.delete
  should throw error if url is not a string!

api.cart
  api.cart.setShippingAddress
    doc mismatch: 'xxx-xxx-xxx' in example phone vs "phoneNumber must fully match regular expression '[0-9]{0,15}'"
      phoneNumber regex is probably dependent on region, what about reflecting field dependencies in docs?
    doc mismatch: response is not real FullOrder, it is wrapped with { result: _, warnings: ... }
  api.cart.chooseShippingMethod
    doc mismatch: response is not real FullOrder, it is wrapped with { result: _, warnings: ... }
  api.cart.updateItems [POST /v1/my/cart/line-items]
    should be called setLineItems
    doc mismatch: response is not real FullOrder, it is wrapped with { result: _, warnings: ... }
    lineItems.skus.totalPrice doesn't take item quantity into calculation (assumes item quantity is always 1)
  api.cart.updateQty
    doc mismatch: response is not real FullOrder, it is wrapped with { result: _, warnings: ... }

orders
  it is possible create order without a shipping method, but impossible to open its details page
  fraudScore inconsistency
    customerApi.cart.checkout -> fraudScore = 0
    adminApi.orders.one -> fraudScore = 7 (for example)

api-js core vs highlevel
  core - direct wrappers around backend endpoints, each core method is exactly one request to server
  highlevel - any combo of wrappers, may send multiple requests in a call

[fixed?] api.giftCards.create
  response is wrapped with [{ success: true, giftCard: : _ }]
api.giftCards.update
  no updatedAt field
  no storeAdmin field (but it is present in response of api.giftCards.create)

api.carts.addWatchers
  only on one gce instance!: says user with such id not found and adds some other user instead

adminApi.carts.removeWatcher
  returns nothing, should it return new list of watchers?

api context parameter ('default' vs ?)
  should it be regular parameter or optional?
  optional is more convenient, but regular will increase awareness

api.albums.update & api.albums.uploadImages
  they do not update updatedAt field of album

DELETE /customers/:cid/addresses/:aid
  returns empty payload {}

api.dev.creditCardToken CreditCardDetailsPayload
  CVV is Int
    probably if it has 0-2 digits it's properly handled
    as having leading zeros and thus this is not an issue
    !!! but at least in '/v1/credit-card-token' it's not handled properly and CVV starting with 0 fail !!!
  http://stackoverflow.com/questions/15914762/cvv-numbers-starting-with-0-zero-fail-during-execution

where is '/v1/inventory/summary/:skuCode' processed?

api.products.update
  slug is not being updated after changing attributes.title - ok?

clientside only stripe.js methods
  api.creditCards.create
  api.creditCards.cardType
  api.creditCards.validateCardNumber
  api.creditCards.validateCVC
  api.creditCards.validateExpiry

terminology
  what is the difference between 'current balance' & 'available balance'?
  what is 'original balance'? (balance of GC/SC when it was created?)
  terminology mismatch
    'add shipping address' vs 'set shipping address'
    'add shipping method' vs 'choose shipping method'
    rhs variants are used in api-js currently (they are probably more accurate)
    lhs variants are used in bvt docs and postman suites (probably somewhere else too)


