import _ from 'lodash';

export const statusTitles = {
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
    return this.orderStatus === 'cart';
  }

  get isRemorseHold() {
    return this.orderStatus === 'remorseHold';
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

  get statusTitle() {
    return statusTitles[this.orderStatus];
  }
};
