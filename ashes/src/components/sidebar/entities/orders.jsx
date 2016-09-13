/* @flow */
import React, { Component, Element } from 'react';
import _ from 'lodash';

import NavigationItem from 'components/sidebar/navigation-item';
import { IndexLink, Link } from 'components/link';

type Props = {
  routes: Object,
  collapsed: boolean,
  status: string,
  toggleMenuItem: Function,
};

export default class OrdersEntry extends Component {
  props: Props;

  render(): Element {
    // TODO: Insert logic that will determine what items show.
    return (
      <li>
        <NavigationItem
          to="orders"
          icon="icon-orders"
          title="Orders"
          isIndex={true}
          isExpandable={true}
          routes={this.props.routes}
          collapsed={this.props.collapsed}
          status={this.props.status}
          toggleMenuItem={this.props.toggleMenuItem}>
          <IndexLink to="carts" className="fc-navigation-item__sublink">Carts</IndexLink>
          <IndexLink to="orders" className="fc-navigation-item__sublink">Orders</IndexLink>
        </NavigationItem>
      </li>
    );
  }
}
