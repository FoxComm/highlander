
/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './cart.css';
import { connect } from 'react-redux';
import classNames from 'classnames';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';

import Currency from 'ui/currency';
import LineItem from './line-item';
import Button from 'ui/buttons';
import Icon from 'ui/icon';

import * as actions from 'modules/cart';

const getState = state => ({ ...state.cart });

class Cart extends Component {

  componentWillMount() {
    this.props.fetch();
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
    const cartClass = classNames({
      'cart-hidden': !this.props.isVisible,
      'cart-shown': this.props.isVisible,
    });

    const checkoutDisabled = _.size(this.props.skus) < 1;

    return (
      <div styleName={cartClass}>
        <div styleName="overlay" onClick={this.props.toggleCart}>
        </div>
        <div styleName="cart-box">
          <div styleName="cart-header" onClick={this.props.toggleCart}>
            <Icon name="fc-chevron-left" styleName="left-icon"/>
            <div styleName="header-text">
              KEEP SHOPPING
            </div>
          </div>
          <div styleName="cart-content">
            <div styleName="line-items">
              {this.lineItems}
            </div>
            <div styleName="cart-subtotal">
              <div styleName="subtotal-title">
                SUBTOTAL
              </div>
              <div styleName="subtotal-price">
                <Currency value={this.props.totals.subTotal} />
              </div>
            </div>
          </div>
          <div styleName="cart-footer">
            <Button onClick={this.onCheckout} disabled={checkoutDisabled} styleName="checkout-button">
              CHECKOUT
            </Button>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(getState, actions)(Cart);
