/* @flow */

import React from 'react';

import { anyPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';

import styles from './entries.css';

const cartClaims = readAction(frn.oms.cart);
const orderClaims = readAction(frn.oms.order);

const OrdersEntry = ({ claims, routes }: TMenuEntry) => {
  const allClaims = { ...cartClaims, ...orderClaims };

  if (!anyPermitted(allClaims, claims)) {
    return <div></div>;
  }

  return (
    <div styleName="fc-entries-wrapper">
      <h3>ORDERS</h3>
      <li>
        <NavigationItem
          to="orders"
          icon="orders"
          title="Orders"
          routes={routes}
          actualClaims={claims}
          expectedClaims={orderClaims}
        />
      </li>
      <li>
        <NavigationItem
          to="carts"
          icon="carts"
          title="Carts"
          routes={routes}
          actualClaims={claims}
          expectedClaims={cartClaims}
        />
      </li>
    </div>
  );
};

export default OrdersEntry;
