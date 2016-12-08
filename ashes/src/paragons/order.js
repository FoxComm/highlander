import _ from 'lodash';

export type Cart = {
  referenceNumber: string,
  shippingMethod: ShippingMethod,
};

export type Order = {
  referenceNumber: string,
  title: string,
  customer: Object,
  promotion: Object,
  coupon: Object,
  paymentMethods: Array<PaymentMethod>,
  orderState: string,
};

export type ShippingMethod = {
  id: number,
};

export type PaymentMethod = {
  id?: number,
  code?: string,
  type: string,
};

export type CreditCard = {
  brand: string,
  holderName: string,
  address: Object,
  expMonth: number,
  expYear: number,
  lastFour: string,
};

export type SkuItem = {
  imagePath: string,
  name: string,
  sku: string,
  price: number,
  quantity: number,
  totalPrice: number,
  attributes: ?Object,
};

export const states = {
  cart: 'cart',
  remorseHold: 'remorseHold',
  manualHold: 'manualHold',
  fraudHold: 'fraudHold',
  fulfillmentStarted: 'fulfillmentStarted',
  canceled: 'canceled',
  partiallyShipped: 'partiallyShipped',
  shipped: 'shipped',
};

export const stateTitles = {
  [states.cart]: 'Cart',
  [states.remorseHold]: 'Remorse Hold',
  [states.manualHold]: 'Manual Hold',
  [states.fraudHold]: 'Fraud Hold',
  [states.fulfillmentStarted]: 'Fulfillment Started',
  [states.canceled]: 'Canceled',
  [states.partiallyShipped]: 'Partially Shipped',
  [states.shipped]: 'Shipped',
};

// this map taken from scala code

export const allowedStateTransitions = {
  [states.cart]: [states.fraudHold, states.remorseHold, states.canceled, states.fulfillmentStarted],
  [states.fraudHold]: [states.manualHold, states.remorseHold, states.fulfillmentStarted, states.canceled],
  [states.remorseHold]: [states.fraudHold, states.manualHold, states.fulfillmentStarted, states.canceled],
  [states.manualHold]: [states.fraudHold, states.remorseHold, states.fulfillmentStarted, states.canceled],
  [states.fulfillmentStarted]: [states.shipped, states.canceled],
};

function collectLineItems(skus: Array<SkuItem>): Array<SkuItem> {
  return _.map(skus, (l: SkuItem) => {
    l.totalPrice = l.quantity * l.price;
    return l;
  });
}

export default class OrderParagon {
  constructor(order) {
    Object.assign(this, order);
    const skus = _.get(order, 'lineItems.skus');
    if (skus) {
      this.lineItems.skus = collectLineItems(skus);
    }
  }

  get entityId() {
    return this.referenceNumber;
  }

  get entityType() {
    return 'order';
  }

  get isCart() {
    return this.orderState === states.cart;
  }

  get isRemorseHold() {
    return this.orderState === states.remorseHold;
  }

  get isNew() {
    return _.isEmpty(this.referenceNumber);
  }

  get refNum() {
    return this.referenceNumber;
  }

  get title() {
    return this.isCart ? 'Cart' : 'Order';
  }

  get stateTitle() {
    return stateTitles[this.orderState];
  }
};
