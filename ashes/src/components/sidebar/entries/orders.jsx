/* @flow */
import React, { Component, Element } from 'react';
import _ from 'lodash';

import { anyPermitted, isPermitted } from 'lib/claims';

import NavigationItem from 'components/sidebar/navigation-item';
import { IndexLink, Link } from 'components/link';

import type { Claims } from 'lib/claims';

type Props = {
  claims: Claims,
  routes: Object,
  collapsed: boolean,
  status: string,
  toggleMenuItem: Function,
};

const cartClaims = { 'frn:oms:cart': ['r'] };
const orderClaims = { 'frn:oms:order': ['r'] };

export default class OrdersEntry extends Component {
  props: Props;

  render(): Element {
    const { claims, collapsed, routes, status, to, toggleMenuItem } = this.props;
    const allClaims = { ...cartClaims, ...orderClaims };

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <li>
        <NavigationItem
          to={to}
          icon="icon-orders"
          title="Orders"
          isIndex={true}
          isExpandable={true}
          routes={routes}
          collapsed={collapsed}
          status={status}
          toggleMenuItem={toggleMenuItem}>
          <IndexLink
            to="carts"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={cartClaims}>
            Carts
          </IndexLink>
          <IndexLink
            to="orders"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={orderClaims}>
            Orders
          </IndexLink>
        </NavigationItem>
      </li>
    );
  }
}
