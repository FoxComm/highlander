/* @flow */

import React from 'react';

import { anyPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';

import styles from './entries.css';

const customerClaims = readAction(frn.user.customer);
const customerGroupClaims = readAction(frn.user.customerGroup);

const CustomersEntry = ({ claims, routes }: TMenuEntry) => {
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
            expectedClaims={customerClaims}
          />
        </li>
        <li>
          <NavigationItem
            to="groups"
            icon="groups"
            title="Customer Groups"
            routes={routes}
            actualClaims={claims}
            expectedClaims={customerGroupClaims}
          />
        </li>
      </div>
    );
};
export default CustomersEntry;
