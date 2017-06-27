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
import OrderTotals from 'components/order-summary/totals';
import Products from 'components/order-summary/product-table';

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

    return (
      <div>
        <Products skus={order.lineItems.skus} confirmationPage />
        <OrderTotals totals={order.totals} />
      </div>
    );
  }

  render() {
    const { t, orderPlaced } = this.props;

    return (
      <div styleName="order-placed">
        <div styleName="thank-you">
          <h1 styleName="title">Thank you for your order!</h1>

          <div styleName="desc">
            <p>{t('We\'ve received your order and we\'ll be packing it up for shipping soon!')}</p>
            <p>{t('Keep your eye out for an email confirmation, which you should receive shortly.')}</p>
          </div>
          <Button styleName="to-home" onClick={this.toHome}>{t('Continue Shopping')}</Button>
        </div>
        <div styleName="order-summary">
          <div styleName="bordered-block">
            <div styleName="order-number-content">
              <h2 styleName="subtitle">Order Confirmation Number</h2>
              <p styleName="order-ref">{orderPlaced}</p>
            </div>
          </div>

          <div styleName="bordered-block">
            {this.orderSummary}
          </div>
        </div>
      </div>
    );
  }
}

export default OrderPlaced;
