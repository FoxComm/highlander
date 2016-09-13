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

export default class CatalogEntry extends Component {
  props: Props;

  render(): Element {
    // TODO: Insert logic that will determine what items show.
    return (
      <li>
        <NavigationItem
          to="products"
          icon="icon-items"
          title="Catalog"
          isIndex={true}
          isExpandable={true}
          routes={this.props.routes}
          collapsed={this.props.collapsed}
          status={this.props.status}
          toggleMenuItem={this.props.toggleMenuItem}>
          <IndexLink to="products" className="fc-navigation-item__sublink">Products</IndexLink>
          <IndexLink to="skus" className="fc-navigation-item__sublink">SKUs</IndexLink>
          <IndexLink to="inventory" className="fc-navigation-item__sublink">Inventory</IndexLink>
        </NavigationItem>
        </li>
    );
  }
}
