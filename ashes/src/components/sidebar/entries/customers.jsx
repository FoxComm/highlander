/* @flow */
import React, { Component, Element } from 'react';

import { isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from 'components/sidebar/navigation-item';
import { IndexLink, Link } from 'components/link';

import type { Claims } from 'lib/claims';

type Props = {
  claims: Claims,
  routes: Array<Object>,
  collapsed: boolean,
  status: string,
  toggleMenuItem: Function,
};

const customerClaims = readAction(frn.user.customer);
const customerGroupClaims = readAction(frn.user.customerGroup);

export default class CustomersEntry extends Component {
  props: Props;

  render() {
    const { claims } = this.props;
    if (!isPermitted(customerClaims, this.props.claims)) {
      return <div />;
    }

    return (
      <li>
        <NavigationItem
          to="customers"
          icon="icon-customers"
          title="Customers"
          isIndex={true}
          isExpandable={true}
          routes={this.props.routes}
          collapsed={this.props.collapsed}
          status={this.props.status}
          toggleMenuItem={this.props.toggleMenuItem}>
          <IndexLink
            to="customers"
            className="fc-navigation-item__sublink">
            Customers
          </IndexLink>
          <IndexLink
            to="groups"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={customerGroupClaims}>
            Customer Groups
          </IndexLink>
        </NavigationItem>
      </li>
    );
  }
}
