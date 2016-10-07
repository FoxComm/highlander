
import React, { Component } from 'react';
import styles from './order-placed.css';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import localized from 'lib/i18n';

import Button from 'ui/buttons';
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

    return (
      <div styleName="order-placed">
        <div styleName="header">{t('Thanks for your order!')}</div>
        <div styleName="order-number">{t('Order Number:')} {orderPlaced}</div>
        <div styleName="desc">
          {t(
            `Looks like you'll be getting some cool stuff soon. ` +
            `In the meantime, we're sending you an email with your order information.`
          )}
        </div>
        <Button styleName="to-home" onClick={this.toHome}>{t('Take me home')}</Button>
      </div>
    );
  }
}

export default OrderPlaced;
