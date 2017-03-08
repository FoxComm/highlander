/* @flow */

import React, { Element } from 'react';

import { anyPermitted, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';
import { IndexLink, Link } from 'components/link';

import type { Claims } from 'lib/claims';

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
