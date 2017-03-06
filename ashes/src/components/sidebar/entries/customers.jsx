/* @flow */
import React, { Component, Element } from 'react';

import { isPermitted, anyPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';
import { IndexLink, Link } from 'components/link';

import type { Claims } from 'lib/claims';

import styles from './entries.css';

type Props = {
  claims: Claims,
  routes: Array<Object>
};

const customerClaims = readAction(frn.user.customer);
const customerGroupClaims = readAction(frn.user.customerGroup);

class CustomersEntry extends Component {
  props: Props;

  render() {
    const { claims, routes } = this.props;
    const allClaims = { ...customerClaims, ...customerGroupClaims };

    if (!anyPermitted(allClaims, claims)) {
      console.log("Not permitted");
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
  }
}
export default CustomersEntry;
