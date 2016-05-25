
export function updateQty(item_id, qty) {  
  return this.patch(`${this.path.editItem}/${item_id}`, { qty })
}

export function addToCart(items) {
  return this.post(this.path.addToCart, items)
}

export function removeFromCart(items) {
  return this.post(this.path.addToCart, items)
}

export function emptyCart() {
  return this.post(this.path.emptyCart)
}
