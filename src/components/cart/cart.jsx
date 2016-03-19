
/* @flow */

import React from 'react';
import styles from './cart.css';

import Currency from 'ui/currency';

const Cart = () => {
  return (
    <div styleName="cart">
      <div styleName="overlay">
      </div>
      <div styleName="cart-box">
        <div styleName="cart-header">
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

export default Cart;
