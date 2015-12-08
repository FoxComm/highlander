import _ from 'lodash';

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
};
