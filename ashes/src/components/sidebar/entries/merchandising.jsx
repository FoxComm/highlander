/* @flow */
import React, { Component, Element } from 'react';

import { anyPermitted, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';
import { IndexLink, Link } from 'components/link';

import type { Claims } from 'lib/claims';

type Props = {
  claims: Claims,
  routes: Array<Object>
};

const taxonomyClaims = readAction(frn.merch.taxonomy);

class MerchandisingEntry extends Component {
  props: Props;

  render() {
    const { claims, routes } = this.props;
    const allClaims = taxonomyClaims;

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <div>
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
  }
}

export default MerchandisingEntry;
