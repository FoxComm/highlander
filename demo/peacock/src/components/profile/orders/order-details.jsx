/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';

// actions
import { fetchOrder, clearOrder } from 'modules/orders';

// components
import Loader from 'ui/loader';
import AddressDetails from 'ui/address/address-details';
import Currency from 'ui/currency';
import OrderSummary from 'components/order-summary/order-summary';
import ViewBilling from 'pages/checkout/billing/view-billing';
import PromoCode from 'components/promo-code/promo-code';

import styles from '../profile.css';

type Props = {
  referenceNumber: string,
  order: Object,
  fetchOrder: (referenceNumber: string) => Promise<*>,
  fetchOrderState: boolean,
  clearOrder: () => void,
};

type State = {
  orderFetched: boolean,
};

class OrderDetails extends Component {
  props: Props;
  state: State = {
    orderFetched: false,
  };

  componentWillMount() {
    const { referenceNumber } = this.props;
    this.props.fetchOrder(referenceNumber).then(() => this.setState({ orderFetched: true }));
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

  get giftCards() {
    const { order } = this.props;
    const methods = _.filter(order.paymentMethods, { type: 'giftCard' });

    if (_.isEmpty(methods)) return null;

    return (
      <div styleName="promo-line">
        <PromoCode
          giftCards={methods}
          allowDelete={false}
        />
      </div>
    );
  }

  get creditCard() {
    const { order } = this.props;
    const paymentMethod = _.find(order.paymentMethods, { type: 'creditCard' });

    if (_.isEmpty(paymentMethod)) return null;

    return (
      <ViewBilling
        billingData={paymentMethod}
      />
    );
  }

  get applePay() {
    const { order } = this.props;
    const paymentMethod = _.find(order.paymentMethods, { type: 'applePay' });

    if (_.isEmpty(paymentMethod)) return null;

    return (
      <div styleName="apple-pay">Apple pay</div>
    );
  }

  get paymentMethods() {
    return (
      <div styleName="payment-method">
        <div styleName="title">Payment</div>
        {this.creditCard}
        {this.giftCards}
        {this.applePay}
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
    const { orderFetched } = this.state;
    const { fetchOrderState } = this.props;

    if (!orderFetched || !fetchOrderState) return <Loader />;

    return (
      <div>
        {this.shippingAddress}
        {this.shippingMethod}
        {this.paymentMethods}
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
})(OrderDetails);
