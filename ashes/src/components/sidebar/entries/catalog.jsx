/* @flow */
import React, { Component, Element } from 'react';

import { anyPermitted, isPermitted } from 'lib/claims';
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

const productClaims = readAction(frn.pim.product);
const skuClaims = readAction(frn.pim.sku);
const inventoryClaims = readAction(frn.mdl.summary);

export default class CatalogEntry extends Component {
  props: Props;

  render(): Element {
    const { claims, collapsed, routes, status, toggleMenuItem } = this.props;
    const allClaims = { ...productClaims, ...skuClaims, ...inventoryClaims };

    if (!anyPermitted(allClaims, claims)) {
      return <div />;
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
