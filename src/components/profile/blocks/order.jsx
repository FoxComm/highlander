
import _ from 'lodash';
import React, { Component } from 'react';
import styles from '../profile.css';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

import Block from '../common/block';
import OrderRow from './order-row';
import Loader from 'ui/loader';
import AddressDetails from 'ui/address/address-details';
import Currency from 'ui/currency';
import Icon from 'ui/icon';

import * as actions from 'modules/orders';

function mapStateToProps(state) {
  return {
    order: state.orders.current,
  };
}


class Order extends Component {
  static title = params => `Order ${params.referenceNumber}`;

  get referenceNumber() {
    return _.get(this.props.routeParams, 'referenceNumber');
  }

  componentWillMount() {
    this.props.fetchOrder(this.referenceNumber);
  }

  componentWillUnmount() {
    this.props.clearOrder();
  }

  get shippingAddress() {
    const { shippingAddress } = this.props.order;

    return <AddressDetails address={shippingAddress} />;
  }

  renderCreditCard(paymentMethod) {
    return (
      <div styleName="payment-method" key={paymentMethod.type}>
        <div>
          <Icon styleName="payment-icon" name={`fc-payment-${paymentMethod.brand.toLowerCase()}`} />
        </div>
        <div>
          <strong>{paymentMethod.holderName}</strong>
        </div>
        <div>
          XXXX-XXXX-XXXX-{paymentMethod.lastFour}
        </div>
        <div>
          {paymentMethod.expMonth}/{paymentMethod.expYear}
        </div>
      </div>
    );
  }

  renderGiftCard(paymentMethod) {
    return (
      <div styleName="payment-method" key={paymentMethod.code}>
        <div>GIFT CARD {paymentMethod.code}</div>
        <div>
          <strong><Currency value={paymentMethod.amount} /></strong>
        </div>
      </div>
    );
  }

  renderStoreCredit(paymentMethod) {
    return (
      <div styleName="payment-method" key={paymentMethod.type}>
        <div>GIFT CARD BALANCE</div>
        <div>
          <strong><Currency value={paymentMethod.amount} /></strong>
        </div>
      </div>
    );
  }

  @autobind
  renderPaymentMethod(paymentMethod) {
    switch (paymentMethod.type) {
      case 'creditCard':
        return this.renderCreditCard(paymentMethod);
      case 'giftCard':
        return this.renderGiftCard(paymentMethod);
      case 'storeCredit':
        return this.renderStoreCredit(paymentMethod);
      default:
        return null;
    }
  }

  render() {
    const { order } = this.props;
    if (!order) {
      return <Loader/>;
    }

    return (
      <Block title={`Order ${order.referenceNumber}`}>
        <table styleName="simple-table">
          <thead>
          <tr>
            <th>Date</th>
            <th>Order #</th>
            <th>Total</th>
            <th>Status</th>
            <th>Tracking</th>
          </tr>
          </thead>
          <tbody>
            <OrderRow order={order} />
          </tbody>
          <tbody styleName="order-service">
            <tr>
              <th colSpan="2">SHIPPING</th>
              <th colSpan="2">DELIVERY</th>
              <th>BILLING</th>
            </tr>
            <tr>
              <td colSpan="2">
                {this.shippingAddress}
              </td>
              <td colSpan="2">
                <div styleName="delivery-name">{order.shippingMethod.name}</div>
                <div>
                  <strong><Currency value={order.shippingMethod.price} /></strong>
                </div>
              </td>
              <td>
                {_.map(order.paymentMethods, this.renderPaymentMethod)}
              </td>
            </tr>
          </tbody>
        </table>
      </Block>
    );
  }
}

export default connect(mapStateToProps, actions)(Order);
