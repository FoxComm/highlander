/*
 @class Cart
 Accessible via [cart](#foxapi-cart) property of [FoxApi](#foxapi) instance.
 */


import _ from 'lodash';
import * as endpoints from '../endpoints';

// reduce SKU list
function collectLineItems(skus) {
  return _.map(skus, (l) => {
    l.totalPrice = l.quantity * l.price;
    return l;
  });
}

function normalizeResponse(payload) {
  if (payload.lineItems) {
    payload.lineItems.skus = collectLineItems(payload.lineItems.skus);
  }
  return payload;
}

export default class Cart {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method getShippingMethods(): Promise<ShippingMethod[]>
   * Returns available shipping methods.
   */
  getShippingMethods() {
    return this.api.get(endpoints.shippingMethods);
  }

  /**
   * @method chooseShippingMethod(shippingMethodId: Number): Promise<FullOrder>
   * Chooses shipping method for the cart.
   */
  chooseShippingMethod(shippingMethodId) {
    return this.api.patch(endpoints.shippingMethods, { shippingMethodId }).then(normalizeResponse);
  }

  /**
   * @method removeShippingMethod(): Promise
   * Removes the shipping method from the cart.
   */
  removeShippingMethod() {
    return this.api.delete(endpoints.shippingMethod);
  }

  /**
   * @method setShippingAddress(shippingAddress: CreateAddressPayload): Promise<FullOrder>
   * Creates shipping address for the cart by a given address payload.
   */
  setShippingAddress(shippingAddress) {
    return this.api.post(endpoints.shippingAddress, shippingAddress).then(normalizeResponse)
  }

  /**
   * @method setShippingAddressById(shippingAddressId: Number): Promise<FullOrder>
   * Creates shipping address for the cart by a given address id.
   */
  setShippingAddressById(shippingAddressId) {
    return this.api.patch(endpoints.shippingAddressId(shippingAddressId)).then(normalizeResponse);
  }

  /**
   * @method updateShippingAddress(shippingAddress: UpdateAddressPayload): Promise<FullOrder>
   * Updates shipping address for the cart.
   */
  updateShippingAddress(shippingAddress) {
    return this.api.patch(endpoints.shippingAddress, shippingAddress).then(normalizeResponse);
  }

  /**
   * @method removeShippingAddress(): Promise<FullOrder>
   * Removes a shipping address from the cart.
   */
  removeShippingAddress() {
    return this.api.delete(endpoints.shippingAddress).then(normalizeResponse);
  }

  /**
   * @method get(): Promise<FullOrder>
   * Returns or creates new cart.
   */
  get() {
    return this.api.get(endpoints.cart).then(normalizeResponse);
  }

  /**
   * @method checkout(): Promise<FullOrder>
   * Place order from cart.
   */
  checkout() {
    return this.api.post(endpoints.cartCheckout).then(normalizeResponse);
  }

  /**
   * @method updateQuantities(itemQuantities: ItemQuantities): Promise<FullOrder>
   */
  updateQuantities(itemQuantities) {
    const updateSkusPayload = _.map(itemQuantities, (quantity, sku) => {
      return {
        sku,
        quantity
      };
    });

    return this.api.post(endpoints.cartLineItems, updateSkusPayload).then(normalizeResponse);
  }

  /**
   * @method updateQty(sku: String, qty: Number): Promise<FullOrder>
   * Updates quantity for selected item in the cart
   */
  updateQty(sku, qty) {
    return this.updateQuantities({
      [sku]: qty
    });
  }

  /**
   * @method addSku(sku: String, quantity: Number): Promise<FullOrder>
   * Adds sku by defined quantity in the cart.
   */
  addSku(sku, quantity) {
    return this.get().then(cart => {
      const skuData = _.find(_.get(cart, 'lineItems.skus', []), { sku });
      const existsQuantity = skuData ? skuData.quantity : 0;

      return this.updateQty(sku, existsQuantity + quantity);
    });
  }

  /**
   * @method removeSku(sku: String): Promise<FullOrder>
   * Removes selected sku from the cart.
   */
  removeSku(sku) {
    return this.updateQty(sku, 0);
  }

  /**
   * @method addCreditCard(creditCardId: Number): Promise<FullOrder>
   * Adds a credit card as payment method for the cart.
   */
  addCreditCard(creditCardId) {
    return this.api.post(endpoints.cartPaymentCreditCarts, { creditCardId }).then(normalizeResponse);
  }

  /**
   * @method removeCreditCards(): Promise<FullOrder>
   * Removes all credit cards payment methods of the cart.
   */
  removeCreditCards() {
    return this.api.delete(endpoints.cartPaymentCreditCarts).then(normalizeResponse);
  }

  /**
   * @method addGiftCard(giftCardPayload: GiftCardPaymentPayload): Promise<FullOrder>
   * Adds a gift card as payment method for the cart.
   */
  addGiftCard(giftCardPayload) {
    return this.api.post(endpoints.cartPaymentGiftCards, giftCardPayload).then(normalizeResponse);
  }

  /**
   * @method removeGiftCards(): Promise<FullOrder>
   * Removes all gift cards payment methods of the cart.
   */
  removeGiftCards() {
    return this.api.delete(endpoints.cartPaymentGiftCards).then(normalizeResponse);
  }

  /**
   * @method addGiftCard(amount: Number): Promise<FullOrder>
   * Creates payment method with a given amount using store credit.
   */
  addStoreCredit(amount) {
    return this.api.post(endpoints.cartPaymentStoreCredits, { amount }).then(normalizeResponse);
  }

  /**
   * @method removeGiftCards(): Promise<FullOrder>
   * Removes all store credits payment methods of the cart.
   */
  removeStoreCrdits() {
    return this.api.delete(endpoints.cartPaymentStoreCredits).then(normalizeResponse);
  }
}

// @miniclass ItemQuantities (Cart)
// @key sku: Number = {'sku-bread': 2}
// Quantity for sku.
