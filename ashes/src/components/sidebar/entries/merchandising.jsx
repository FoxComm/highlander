/* @flow */

import React from 'react';

import { anyPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';

import styles from './entries.css';

const taxonomyClaims = readAction(frn.merch.taxonomy);

const MerchandisingEntry = ({ claims, routes }: TMenuEntry) => {
    const allClaims = taxonomyClaims;

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <div styleName="fc-entries-wrapper">
        <h3>MERCHANDISING</h3>
        <li>
          <NavigationItem
            to="taxonomies"
            icon="taxonomies"
            title="Taxonomies"
            routes={routes}
            actualClaims={claims}
            expectedClaims={taxonomyClaims}
          />
        </li>
      </div>
    );
};

export default MerchandisingEntry;
