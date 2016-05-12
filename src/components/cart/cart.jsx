
/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './cart.css';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';

import localized from 'lib/i18n';

import Currency from 'ui/currency';
import LineItem from './line-item';
import Button from 'ui/buttons';
import Icon from 'ui/icon';

import * as actions from 'modules/cart';

const getState = state => ({ ...state.cart, ...state.auth, checkoutStage: state.checkout.editStage });

class Cart extends Component {

  componentWillMount() {
    /** prevent loading if no user logged in and when checkout is in progress */
    if (this.props.user && !_.isNumber(this.props.checkoutStage)) {
      this.props.fetch();
    }
  }

  get lineItems() {
    return _.map(this.props.skus, sku => {
      return <LineItem {...sku} deleteLineItem={this.props.deleteLineItem} key={sku.sku} />;
    });
  }

  @autobind
  onCheckout() {
    browserHistory.push('/checkout');
  }

  render() {
    const { props } = this;
    const { t } = props;
    const cartClass = classNames({
      'cart-hidden': !props.isVisible,
      'cart-shown': props.isVisible,
    });

    const checkoutDisabled = _.size(props.skus) < 1;

    return (
      <div styleName={cartClass}>
        <div styleName="overlay" onClick={props.toggleCart}>
        </div>
        <div styleName="cart-box">
          <div styleName="cart-header" onClick={props.toggleCart}>
            <Icon name="fc-chevron-left" styleName="back-icon"/>
            <div styleName="header-text">
              {t('KEEP SHOPPING')}
            </div>
          </div>
          <div styleName="cart-content">
            <div styleName="line-items">
              {this.lineItems}
            </div>
            <div styleName="cart-subtotal">
              <div styleName="subtotal-title">
                {t('SUBTOTAL')}
              </div>
              <div styleName="subtotal-price">
                <Currency value={props.totals.subTotal} />
              </div>
            </div>
          </div>
          <div styleName="cart-footer">
            <Button onClick={this.onCheckout} disabled={checkoutDisabled} styleName="checkout-button">
              {t('CHECKOUT')}
            </Button>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(getState, actions)(localized(Cart));
