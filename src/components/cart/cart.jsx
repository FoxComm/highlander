
/* @flow */

import React from 'react';
import styles from './cart.css';
import { connect } from 'react-redux';
import classNames from 'classnames';

import Currency from 'ui/currency';

import * as actions from 'modules/cart';

const getState = state => ({ ...state.cart });

const Cart = (props) => {
  const cartClass = classNames({
    'cart-hidden': !props.isVisible,
    'cart-shown': props.isVisible,
  });

  return (
    <div styleName={cartClass}>
      <div styleName="overlay">
      </div>
      <div styleName="cart-box">
        <div styleName="cart-header" onClick={props.toggleCart}>
            KEEP SHOPPING
        </div>
        <div styleName="cart-content">
          <div styleName="cart-subtotal">
            <div styleName="subtotal-title">
              SUBTOTAL
            </div>
            <div styleName="subtotal-price">
              <Currency value={15900} />
            </div>
          </div>
        </div>
        <div styleName="cart-footer">
          CHECKOUT
        </div>
      </div>
    </div>
  );
};

export default connect(getState, actions)(Cart);
