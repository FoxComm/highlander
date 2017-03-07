/* @flow */

import React, { Element } from 'react';

import { anyPermitted, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';
import { IndexLink, Link } from 'components/link';

import type { Claims } from 'lib/claims';

import styles from './entries.css';

const taxonomyClaims = readAction(frn.merch.taxonomy);

const MerchandisingEntry = ({ claims, routes }: { claims: Claims, routes: Array<Object> }) => {
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
            expectedClaims={taxonomyClaims} />
        </li>
      </div>
    );
};

export default MerchandisingEntry;
