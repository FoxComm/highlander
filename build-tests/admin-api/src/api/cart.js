/*
 @class Cart
 Accessible via [cart](#foxapi-cart) property of [FoxApi](#foxapi) instance.
 */

import _ from 'lodash';
import endpoints from '../endpoints';

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
    return this.api.get(endpoints.customerCartShippingMethods);
  }

  /**
   * @method chooseShippingMethod(shippingMethodId: Number): Promise<FullOrder>
   * Chooses shipping method for the cart.
   */
  chooseShippingMethod(shippingMethodId) {
    return this.api.patch(endpoints.customerCartShippingMethod, { shippingMethodId }).then(normalizeResponse);
  }

  /**
   * @method removeShippingMethod(): Promise
   * Removes the shipping method from the cart.
   */
  removeShippingMethod() {
    return this.api.delete(endpoints.customerCartShippingMethod);
  }

  /**
   * @method setShippingAddress(shippingAddress: CreateAddressPayload): Promise<FullOrder>
   * Creates shipping address for the cart by a given address payload.
   */
  setShippingAddress(shippingAddress) {
    return this.api.post(endpoints.customerCartShippingAddress, shippingAddress).then(normalizeResponse)
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
    return this.api.patch(endpoints.customerCartShippingAddress, shippingAddress).then(normalizeResponse);
  }

  /**
   * @method removeShippingAddress(): Promise<FullOrder>
   * Removes a shipping address from the cart.
   */
  removeShippingAddress() {
    return this.api.delete(endpoints.customerCartShippingAddress).then(normalizeResponse);
  }

  /**
   * @method get(): Promise<FullOrder>
   * Returns or creates new cart.
   */
  get() {
    return this.api.get(endpoints.customerCart).then(normalizeResponse);
  }

  /**
   * @method checkout(): Promise<FullOrder>
   * Place order from cart.
   */
  checkout() {
    return this.api.post(endpoints.customerCartCheckout).then(normalizeResponse);
  }

  /**
   * @method updateItems(itemsUpdatePayload: ItemsUpdatePayload): Promise<FullOrder>
   */
  updateItems(itemsUpdatePayload) {
    const updateSkusPayload = _.map(itemsUpdatePayload, (updatePayload, sku) => {
      return {
        sku,
        ...updatePayload,
      };
    });

    return this.api.post(endpoints.customerCartLineItems, updateSkusPayload).then(normalizeResponse);
  }

  /**
   * @method updateQty(sku: String, quantity: Number, attributes?: ItemAttributes): Promise<FullOrder>
   * Updates quantity and optionally attributes for selected item in the cart
   */
  updateQty(sku, quantity , attributes = {}) {
    return this.updateItems({
      [sku]: {
        attributes,
        quantity,
      }
    });
  }

  /**
   * @method addSku(sku: String, quantity: Number, attributes?: ItemAttributes): Promise<FullOrder>
   * Adds sku by defined quantity in the cart.
   */
  addSku(sku, quantity, attributes = {}) {
    return this.get().then(cart => {
      const skuData = _.find(_.get(cart, 'lineItems.skus', []), { sku });
      const existsQuantity = skuData ? skuData.quantity : 0;

      return this.updateQty(sku, existsQuantity + quantity, attributes);
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
    return this.api.post(endpoints.customerCartPaymentCreditCards, { creditCardId }).then(normalizeResponse);
  }

  /**
   * @method removeCreditCards(): Promise<FullOrder>
   * Removes all credit cards payment methods of the cart.
   */
  removeCreditCards() {
    return this.api.delete(endpoints.customerCartPaymentCreditCards).then(normalizeResponse);
  }

  /**
   * @method addGiftCard(giftCardPayload: GiftCardPaymentPayload): Promise<FullOrder>
   * Adds a gift card as payment method for the cart.
   */
  addGiftCard(giftCardPayload) {
    return this.api.post(endpoints.customerCartPaymentGiftCards, giftCardPayload).then(normalizeResponse);
  }

  /**
   * @method removeGiftCard(giftCardCode: string): Promise<FullOrder>
   * Removes gift card with provided code payment method from the cart.
   */
  removeGiftCard(giftCardCode) {
    return this.api.delete(endpoints.customerCartPaymentGiftCardsWithCode(giftCardCode)).then(normalizeResponse);
  }

  /**
   * @method addStoreCredit(amount: Number): Promise<FullOrder>
   * Add store credit to customer's cart.
   */
  addStoreCredit(amount) {
    return this.api.post(endpoints.customerCartPaymentStoreCredit, { amount });
  }

  /**
   * @method removeStoreCredits(): Promise<FullOrder>
   * Removes all store credits from cart.
   */
  removeStoreCredit() {
    return this.api.delete(endpoints.customerCartPaymentStoreCredit);
  }


  /**
   * @method addCoupon(code: Number): Promise<FullOrder>
   * Adds coupon code to the cart.
   */
  addCoupon(code) {
    return this.api.post(endpoints.customerCartPaymentCouponCodeWithCode(code.trim()), {}).then(normalizeResponse);
  }

  /**
   * @method removeCoupon(): Promise<FullOrder>
   * Removes coupon code of the cart.
   */
  removeCoupon() {
    return this.api.delete(endpoints.customerCartPaymentCouponCode).then(normalizeResponse);
  }
}

/*
 * @miniclass ItemGiftCard (Cart)
 * @field senderName: String
 * name of the person that is sending the giftcard
 *
 * @field recipientName: String
 * name of the person that is receiving the giftcard
 *
 * @field recipientEmail: String
 * recipient email address
 *
 * @field message?: String
 * optional message to deliver to the recipient
 */

/*
 * @miniclass ItemAttributes (Cart)
 * @field giftcard?: ItemGiftCard
 * giftcard to send attached to a lineItem
 */

// @miniclass ItemUpdatePayload (Cart)
// @field quantity: Number
// New quantity for sku
//
// @field attributes?: ItemAttributes
// New attributes for sku

// @miniclass ItemsUpdatePayload (Cart)
// @key sku: ItemUpdatePayload = {'sku-bread': {quantity: 2}}
// Update payload for sku
