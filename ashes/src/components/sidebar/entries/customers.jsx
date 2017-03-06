/* @flow */
import React, { Component, Element } from 'react';

import { isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';
import { IndexLink, Link } from 'components/link';

import type { Claims } from 'lib/claims';

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
    if (!isPermitted(customerClaims, this.props.claims)) {
      return <div></div>;
    }

    return (
      <div>
        <h3>CUSTOMERS</h3>
        <li>
          <NavigationItem
            to="customers"
            icon="customers"
            title="Customers"
            routes={routes} />
        </li>
        <li>
          <NavigationItem
            to="groups"
            icon="groups"
            title="Customer Groups"
            routes={routes} />
        </li>
      </div>
    );
  }
}
export default CustomersEntry;
