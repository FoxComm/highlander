/* @flow */

// libs
import React, { Component } from 'react';
import { browserHistory } from 'lib/history';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import localized from 'lib/i18n';

// components
import Button from 'ui/buttons';
import Loader from 'ui/loader';
import OrderSummary from 'components/order-summary/order-summary';

// styles
import styles from './order-placed.css';

// actions
import { fetch as fetchCart } from 'modules/cart';
import { resetCheckout } from 'modules/checkout';
import * as actions from 'modules/orders';

function mapStateToProps(state) {
  return {
    orderPlaced: state.checkout.orderPlaced,
    order: state.orders.current,
  };
}

@connect(mapStateToProps, {fetchCart, resetCheckout, ...actions})
@localized
class OrderPlaced extends Component {

  @autobind
  toHome() {
    this.props.fetchCart();
    browserHistory.push('/');
  }

  componentDidMount() {
    const { orderPlaced } = this.props;
    if (orderPlaced) {
      this.props.fetchOrder(orderPlaced);
    }
  }

  componentWillUnmount() {
    this.props.resetCheckout();
    this.props.clearOrder();
  }

  get orderSummary() {
    const { orderPlaced, order } = this.props;
    if (!orderPlaced) {
      return null;
    }

    if (!order) {
      return <Loader />;
    }

    const header = (
      <h2 styleName="subtitle">Your Order</h2>
    );

    return (
      <OrderSummary
        isCollapsed={false}
        header={header}
        styleName="summary"
        {...order}
        skus={order.lineItems.skus}
        orderPlaced
      />
    );
  }

  render() {
    const { t, orderPlaced } = this.props;

    return (
      <div styleName="order-placed">
        <div styleName="thank-you">
          <h1 styleName="title">Thank you for your order!</h1>

          <div styleName="desc">
            <p>We&#39;ve received your order and we&#39;ll be packaging it up to ship out soon!</p>
            <p>Keep your eye out for an email confirmation, which you should receive shortly.</p>
          </div>
          <Button styleName="to-home" onClick={this.toHome}>{t('Continue Shopping')}</Button>
        </div>
        <div styleName="order-summary">
          <div styleName="order-number">
            <div styleName="order-number-content">
              <h2 styleName="subtitle">Order Confirmation Number</h2>
              <p styleName="order-ref">{orderPlaced}</p>
            </div>
          </div>

          {this.orderSummary}
        </div>
      </div>
    );
  }
}

export default OrderPlaced;
