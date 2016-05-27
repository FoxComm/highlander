
// @namespace FoxApi
// @section Cart methods

/**
 * @method updateQty(itemId: String, qty: Number): Promise
 * Updates quantity for selected item in the cart
 */
export function updateQty(itemId, qty) {
  return this.patch(`${this.path.editItem}/${itemId}`, { qty })
}

/**
 * @method addToCart(items: Array): Promise
 * Add new items to the cart
 */
export function addToCart(items) {
  return this.post(this.path.addToCart, items)
}

/**
 * @method removeFromCart(items: Array): Promise
 * Remove items from the cart
 */
export function removeFromCart(items) {
  return this.post(this.path.addToCart, items)
}

/**
 * @method emptyCart(): Promise
 * Erase all items from the cart
 */
export function emptyCart() {
  return this.post(this.path.emptyCart)
}
