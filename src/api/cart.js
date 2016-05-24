
// Need to move cart data marshalling functions from
// https://github.com/FoxComm/firebird/blob/master/src/node_modules/modules/cart.js#L40-L107

function removeFromCart(items, cart) {}
function emptyCart() {}

export function addToCart(items, cart) {
  // TODO: should handle items as either
  // { item }
  // or
  // [ { item }, { item }]
  // should detect & normalize data, same for `removeFromCart()`

  // Weird. We have to hold or pass the full current cart state
  // so that we can marshall the data to the new desired state. TBD
  // see: https://github.com/FoxComm/api-js/issues/2
  // cartUtils.marshallItems(items)  // eg.
  return this.post('/my/cart/line-items')
}
