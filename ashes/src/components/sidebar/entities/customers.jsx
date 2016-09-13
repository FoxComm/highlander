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

export default class CustomersEntry extends Component {
  props: Props;

  render(): Element {
    // TODO: Insert logic that will determine what items show.
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
          <IndexLink to="customers" className="fc-navigation-item__sublink">Customers</IndexLink>
          <IndexLink to="groups" className="fc-navigation-item__sublink">Customer Groups</IndexLink>
        </NavigationItem>
      </li>
    );
  }
}
