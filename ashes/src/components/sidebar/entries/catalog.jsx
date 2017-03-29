/* @flow */

import React from 'react';

import { anyPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';

import styles from './entries.css';

const productClaims = readAction(frn.pim.product);
const skuClaims = readAction(frn.pim.sku);
const inventoryClaims = readAction(frn.mdl.summary);

const CatalogEntry = ({ claims, routes }: TMenuEntry) => {
    const allClaims = { ...productClaims, ...skuClaims, ...inventoryClaims };

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <div styleName="fc-entries-wrapper">
        <h3>CATALOG</h3>
        <li>
          <NavigationItem
            to="products"
            icon="products"
            title="Products"
            routes={routes}
            actualClaims={claims}
            expectedClaims={productClaims}
          />
        </li>
        <li>
          <NavigationItem
            to="skus"
            icon="skus"
            title="SKUs"
            routes={routes}
            actualClaims={claims}
            expectedClaims={skuClaims}
          />
        </li>
        <li>
          <NavigationItem
            to="inventory"
            icon="skus"
            title="Inventory"
            routes={routes}
            actualClaims={claims}
            expectedClaims={inventoryClaims}
          />
        </li>
      </div>
    );
};

export default CatalogEntry;
