import _ from 'lodash';

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

export default class Order {
  constructor(order) {
    Object.assign(this, order);
  }

  get entityId() {
    return this.id;
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
