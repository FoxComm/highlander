/* @flow */

import React, { Element } from 'react';

// TODO: Make this a cart-link when we have the cart routes.
import OrderLink from './order-link';

type Props = {
  cart: {
    referenceNumber: string,
  },
};

const CartTarget = (props: Props): Element => {
  const { cart } = props;
  return (
    <span>
      cart <OrderLink order={cart} />
    </span>
  );
};

export default CartTarget;
