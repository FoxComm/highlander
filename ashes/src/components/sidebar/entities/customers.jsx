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

const requiredClaims = { 'frn:usr:customer': ['r'] };

export default class CustomersEntry extends Component {
  props: Props;

  get customerGroupsLink(): Element {
    const claim = { 'frn:usr:customer-groups': ['r'] };
    if (isPermitted(requiredClaims, claim)) {
      return (
        <IndexLink to="groups" className="fc-navigation-item__sublink">Customer Groups</IndexLink>
      );
    }

    return <div></div>;
  }

  render(): Element {
    if (isPermitted(requiredClaims, this.props.claims)) {
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
            {this.customerGroupsLink}
          </NavigationItem>
        </li>
      );
    } else {
      return <div></div>;
    }
  }
}
