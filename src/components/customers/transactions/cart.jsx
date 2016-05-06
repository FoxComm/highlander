
/* @flow */

import React, { PropTypes, Component, Element } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';

import TotalsSummary from '../../common/totals';
import OrderLineItems from '../../orders/order-line-items';
import { PrimaryButton } from '../../common/buttons';
import OrderShippingAddress from '../../orders/shipping-address';
import OrderShippingMethod from '../../orders/order-shipping-method';
import Payments from '../../orders/payments';
import WaitAnimation from '../../common/wait-animation';
import OrderDiscounts from '../../orders/order-discounts';
import OrderCoupons from '../../orders/order-coupons';

import * as orderActions from '../../../modules/orders/details';
import * as paymentMethodActions from '../../../modules/orders/payment-methods';

type Order = {
  currentOrder: Object,
  validations: Object,
};

type Params = {
  customerId: number;
};

type Props = {
  params: Params,
  order: Order,
  fetchCustomerCart: Function,
  failed: bool,
  isFetching: bool,
};

const mapStateToProps = (state) => {
  return {
    order: state.orders.details,
    lineItems: state.orders.lineItems,
    skuSearch: state.orders.skuSearch,
    shippingMethods: state.orders.shippingMethods,
    skusActions: state.skusActions,
    payments: state.orders.paymentMethods,
    isFetching: state.orders.details.isFetching,
    failed: state.orders.details.failed,
  };
};

const mapDispatchToProps = {
  ...orderActions,
  ...paymentMethodActions
};

/* ::`*/
@connect(mapStateToProps, mapDispatchToProps)
/* ::`*/
export default class CustomerCart extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchCustomerCart(this.props.params.customerId);
  }

  get order(): Object {
    return this.props.order.currentOrder;
  }

  @autobind
  editCart() {
    if (this.order) {
      transitionTo('order', { order: this.order.referenceNumber });
    }
  }

  render() {
    let content;

    if (this.props.failed) {
      content = this.errorMessage;
    } else if (this.props.isFetching) {
      content = this.waitAnimation;
    } else if (_.isEmpty(this.props.order.currentOrder)) {
      content = this.emptyMessage;
    } else {
      content = this.content;
    }

    return (
      <div className="fc-customer-cart">
        {content}
      </div>
    );
  }

  get waitAnimation(): Element {
    return <WaitAnimation/>;
  }

  get errorMessage(): Element {
    return <div className="fc-customer__empty-messages">An error occurred. Try again later.</div>;
  }

  get emptyMessage(): Element {
    return <div className="fc-customer__empty-messages">No current order found for customer.</div>;
  }

  get content(): Element {
    const props = this.props;
    const order = props.order.currentOrder;

    const {
      itemsStatus,
      shippingAddressStatus,
      shippingMethodStatus,
      paymentMethodStatus
    } = props.order.validations;

    return (
      <div>
        <div className="_header">
          <div className="fc-subtitle">Cart {order.referenceNumber}</div>
          <div className="fc-customer-cart__edit-btn">
            <PrimaryButton onClick={this.editCart}>Edit Cart</PrimaryButton>
          </div>
        </div>
        <div className="fc-order-details">
          <div className="fc-order-details-body">
            <div className="fc-order-details-main">
              <OrderLineItems readOnly={true} isCart={false} status={itemsStatus} {...props} />
              <OrderDiscounts readOnly={true} isCart={false} order={order} />
              <OrderShippingAddress readOnly={true} isCart={false} status={shippingAddressStatus} order={order}/>
              <OrderShippingMethod readOnly={true} isCart={false} status={shippingMethodStatus} {...props} />
              <OrderCoupons readOnly={true} isCart={false} order={order} />
              <Payments readOnly={true} isCart={false} status={paymentMethodStatus} {...props} />
            </div>
            <div className="fc-order-details-aside">
              <TotalsSummary entity={order} title={order.title}/>
            </div>
          </div>
        </div>
      </div>
    );
  }
}
