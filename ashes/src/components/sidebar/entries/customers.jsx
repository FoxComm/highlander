/* @flow */

import React, { Element } from 'react';

import { anyPermitted, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';
import { IndexLink, Link } from 'components/link';

import type { Claims } from 'lib/claims';

import styles from './entries.css';

const customerClaims = readAction(frn.user.customer);
const customerGroupClaims = readAction(frn.user.customerGroup);

const CustomersEntry = ({ claims, routes }: { claims: Claims, routes: Array<Object> }) => {
    const allClaims = { ...customerClaims, ...customerGroupClaims };

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <div styleName="fc-entries-wrapper">
        <h3>CUSTOMERS</h3>
        <li>
          <NavigationItem
            to="customers"
            icon="customers"
            title="Customers"
            routes={routes}
            actualClaims={claims}
            expectedClaims={customerClaims} />
        </li>
        <li>
          <NavigationItem
            to="groups"
            icon="groups"
            title="Customer Groups"
            routes={routes}
            actualClaims={claims}
            expectedClaims={customerGroupClaims} />
        </li>
      </div>
    );
};
export default CustomersEntry;
