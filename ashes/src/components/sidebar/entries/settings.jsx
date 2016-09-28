/* @flow */
import React, { Component, Element } from 'react';

import { anyPermitted, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from 'components/sidebar/navigation-item';
import { IndexLink, Link } from 'components/link';

type Props = {
  routes: Array<Object>,
  collapsed: boolean,
  status: string,
  toggleMenuItem: Function,
};

const userClaims = readAction(frn.settings.user);
const pluginClaims = readAction(frn.settings.plugin);
const applicationClaims = readAction(frn.settings.application);

export default class SettingsEntry extends Component {
  props: Props;

  render(): Element {
    const { claims, collapsed, routes, status, toggleMenuItem } = this.props;
    const allClaims = { ...userClaims, ...pluginClaims };

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <li>
        <NavigationItem
          to="users"
          icon="icon-settings"
          title="Settings"
          isIndex={true}
          isExpandable={true}
          routes={routes}
          collapsed={collapsed}
          status={status}
          toggleMenuItem={toggleMenuItem}>
          <IndexLink
            to="users"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={userClaims}>
            Users
          </IndexLink>
          <Link
            to="plugins"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={pluginClaims}>
            Plugins
          </Link>
          <Link
            to="applications"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={applicationClaims}>
            Applications
          </Link>
        </NavigationItem>
      </li>
    );
  }
}
