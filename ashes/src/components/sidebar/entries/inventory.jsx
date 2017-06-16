/* @flow */

import React from 'react';

import { anyPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';

import styles from './entries.css';

const summaryClaims = readAction(frn.mdl.summary);

const InventoryEntry = ({ claims, routes }: TMenuEntry) => {
  const allClaims = { ...summaryClaims };

  if (!anyPermitted(allClaims, claims)) {
    return <div></div>;
  }

  return (
    <div styleName="fc-entries-wrapper">
      <h3>INVENTORY</h3>
      <li>
        <NavigationItem
          to="inventory"
          icon="inventory"
          title="Inventory Summaries"
          routes={routes}
          actualClaims={claims}
          expectedClaims={summaryClaims}
        />
      </li>
    </div>
  );
};

export default InventoryEntry;
