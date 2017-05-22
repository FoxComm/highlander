/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// actions
import { fetchOrder, clearOrder } from 'modules/orders';

// components
import OrderRow from './order-row';
import Loader from 'ui/loader';
import AddressDetails from 'ui/address/address-details';
import Currency from 'ui/currency';
import Icon from 'ui/icon';
import OrderSummary from 'components/order-summary/order-summary';
import ViewBilling from 'pages/checkout/billing/view-billing';

import styles from '../profile.css';

type Props = {
  referenceNumber: string,
  order: Object,
  fetchOrder: (referenceNumber: string) => Promise<*>,
  fetchOrderState: boolean,
  clearOrder: () => void,
};

class Order extends Component {
  props: Props;

  componentWillMount() {
    const { referenceNumber } = this.props;
    this.props.fetchOrder(referenceNumber);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.referenceNumber !== nextProps.referenceNumber) {
      this.props.fetchOrder(nextProps.referenceNumber);
    }
  }

  componentWillUnmount() {
    this.props.clearOrder();
  }

  get shippingAddress() {
    const { shippingAddress } = this.props.order;

    return (
      <div styleName="shipping-address">
        <div styleName="title">Shipping address</div>
        <AddressDetails
          address={shippingAddress}
        />
      </div>
    );
  }

  get shippingMethod() {
    const { order } = this.props;

    return (
      <div styleName="shipping-method">
        <div styleName="title">Shipping method</div>
        <div styleName="shipping-method-data">
          <div>{order.shippingMethod.name}</div>
          <Currency
            value={order.shippingMethod.price}
            styleName="currency"
          />
        </div>
      </div>
    );
  }

  get paymentMethod() {
    const { order } = this.props;
    const paymentMethod = _.filter(order.paymentMethods, { type: 'creditCard' })[0];
    const methodEmpty = _.isEmpty(paymentMethod);

    return (
      <div styleName="payment-method">
        <div styleName="title">Payment</div>
        {
          methodEmpty ?
          <div>No credit card found</div>
          :
          <ViewBilling
            billingData={paymentMethod}
          />
        }
      </div>
    );
  }

  get orderSummary() {
    const { order } = this.props;

    return (
      <OrderSummary
        embedded
        styleName="order-summary"
        skus={order.lineItems.skus}
        {...order}
      />
    );
  }

  get content() {
    const { fetchOrderState } = this.props;

    if (!fetchOrderState) return <Loader />;

    return (
      <div>
        {this.shippingAddress}
        {this.shippingMethod}
        {this.paymentMethod}
        {this.orderSummary}
      </div>
    );
  }

  render() {
    return (
      <div>
        {this.content}
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    order: _.get(state.orders, 'current', {}),
    fetchOrderState: _.get(state.asyncActions, 'fetchOrder.finished', false),
  };
};

export default connect(mapStateToProps, {
  fetchOrder,
  clearOrder,
})(Order);
