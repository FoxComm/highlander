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

const taxonomyClaims = readAction(frn.merch.taxonomy);

export default class MerchandisingEntry extends Component {
  props: Props;

  render() {
    const { claims, collapsed, routes, status, toggleMenuItem } = this.props;
    const allClaims = taxonomyClaims;

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <li>
        <NavigationItem
          to="taxonomies"
          icon="icon-hierarchy"
          title="Merchandising"
          isIndex={true}
          isExpandable={true}
          routes={routes}
          collapsed={collapsed}
          status={status}
          toggleMenuItem={toggleMenuItem}>
          <IndexLink
            to="taxonomies"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={taxonomyClaims}>
            Taxonomies
          </IndexLink>
        </NavigationItem>
      </li>
    );
  }
}

