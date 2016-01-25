import _ from 'lodash';

export const stateTitles = {
  cart: 'Cart',
  remorseHold: 'Remorse Hold',
  manualHold: 'Manual Hold',
  fraudHold: 'Fraud Hold',
  fulfillmentStarted: 'Fulfillment Started',
  canceled: 'Canceled',
  partiallyShipped: 'Partially Shipped',
  shipped: 'Shipped',
};

export default class Order {
  constructor(order) {
    Object.assign(this, order);
  }

  get entityId() {
    return this.referenceNumber;
  }

  get entityType() {
    return 'order';
  }

  get isCart() {
    return this.orderState === 'cart';
  }

  get isRemorseHold() {
    return this.orderState === 'remorseHold';
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
