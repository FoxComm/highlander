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

const productClaims = { 'frn:pim:product': ['r'] };
const skuClaims = { 'frn:pim:sku': ['r'] };
const inventoryClaims = { 'frn:mdl:summary': ['r'] };

export default class CatalogEntry extends Component {
  props: Props;

  render(): Element {
    const { claims, collapsed, routes, status, toggleMenuItem } = this.props;
    const allClaims = { ...productClaims, ...skuClaims, ...inventoryClaims };

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <li>
        <NavigationItem
          to="products"
          icon="icon-items"
          title="Catalog"
          isIndex={true}
          isExpandable={true}
          routes={routes}
          collapsed={collapsed}
          status={status}
          toggleMenuItem={toggleMenuItem}>
          <IndexLink
            to="products"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={productClaims}>
            Products
          </IndexLink>
          <IndexLink
            to="skus"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={skuClaims}>
            SKUs
          </IndexLink>
          <IndexLink
            to="inventory"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={inventoryClaims}>
            Inventory
          </IndexLink>
        </NavigationItem>
      </li>
    );
  }
}
