/* @flow */

import React from 'react';

import { anyPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';

import styles from './entries.css';

const catalogClaims = readAction(frn.pim.catalog);
const productClaims = readAction(frn.pim.product);
const skuClaims = readAction(frn.pim.sku);

const CatalogEntry = ({ claims, routes }: TMenuEntry) => {
  const allClaims = { ...productClaims, ...skuClaims };

  if (!anyPermitted(allClaims, claims)) {
    return <div></div>;
  }

  return (
    <div styleName="fc-entries-wrapper">
      <h3>CATALOG</h3>
      <li>
        <NavigationItem
          to="catalogs"
          icon="catalog"
          title="Catalogs"
          routes={routes}
          actualClaims={claims}
          expectedClaims={catalogClaims}
        />
      </li>
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
    </div>
  );
};

export default CatalogEntry;
