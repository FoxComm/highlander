/* @flow */
import React, { Component, Element } from 'react';
import _ from 'lodash';

import { isPermitted } from 'lib/claims';

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

const cartClaim = { 'frn:oms:cart': ['r'] };
const orderClaim = { 'frn:oms:order': ['r'] };

export default class OrdersEntry extends Component {
  props: Props;

  get cartLink(): ?Element {
    if (isPermitted(cartClaim, this.props.claims)) {
      return <IndexLink to="carts" className="fc-navigation-item__sublink">Carts</IndexLink>;
    }
  }

  get orderLink(): ?Element {
    if (isPermitted(orderClaim, this.props.claims)) {
      return <IndexLink to="orders" className="fc-navigation-item__sublink">Orders</IndexLink>;
    }
  }

  render(): Element {
    const cartLink = this.cartLink;
    const orderLink = this.orderLink;

    const links = [];
    if (cartLink) links.push(cartLink);
    if (orderLink) links.push(orderLink);

    if (links.length == 0) {
      return <div></div>;
    }

    const to = orderLink ? 'orders' : 'carts';

    return (
      <li>
        <NavigationItem
          to={to}
          icon="icon-orders"
          title="Orders"
          isIndex={true}
          isExpandable={true}
          routes={this.props.routes}
          collapsed={this.props.collapsed}
          status={this.props.status}
          toggleMenuItem={this.props.toggleMenuItem}>
          {links}
        </NavigationItem>
      </li>
    );
  }
}
