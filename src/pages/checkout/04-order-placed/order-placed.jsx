
// libs
import React, { Component } from 'react';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import localized from 'lib/i18n';

// components
import Button from 'ui/buttons';
import OrderSummary from '../summary/order-summary';

// styles
import styles from './order-placed.css';

// actions
import { fetch as fetchCart } from 'modules/cart';
import { resetCheckout } from 'modules/checkout';

@connect(state => state.checkout, {fetchCart, resetCheckout})
@localized
class OrderPlaced extends Component {

  @autobind
  toHome() {
    this.props.fetchCart();
    browserHistory.push('/');
  }

  componentWillUnmount() {
    this.props.resetCheckout();
  }

  render() {
    const { t, orderPlaced } = this.props;

    const header = (
      <h2 styleName="subtitle">Your Order</h2>
    );

    return (
      <div styleName="order-placed">
        <div styleName="thank-you">
          <h1 styleName="title">Thank you for your order!</h1>
          <div styleName="order-number">
            <h2 styleName="subtitle">ORDER CONFIRMATION NUMBER</h2>
            <strong>{orderPlaced}</strong>
          </div>
          <div styleName="desc">
            <p>We’ve received your order and we’re packing up some tasty food!</p>
            <p>Keep your eye out for an email confirmation headed your way shortly.</p>
          </div>
          <Button styleName="to-home" onClick={this.toHome}>{t('Take me home')}</Button>
        </div>

        <OrderSummary
          isCollapsed={false}
          header={header}
          styleName="summary"
        />
      </div>
    );
  }
}

export default OrderPlaced;
