/* @flow */
import React, { Component, Element } from 'react';
import _ from 'lodash';

import NavigationItem from 'components/sidebar/navigation-item';
import { IndexLink, Link } from 'components/link';

type Props = {
  routes: Array<Object>,
  collapsed: boolean,
  status: string,
  toggleMenuItem: Function,
};

export default class SettingsEntry extends Component {
  props: Props;

  render(): Element {
    // TODO: Insert logic that will determine what items show.
    return (
      <li>
        <NavigationItem
          to="users"
          icon="icon-settings"
          title="Settings"
          isIndex={true}
          isExpandable={true}
          routes={this.props.routes}
          collapsed={this.props.collapsed}
          status={this.props.status}
          toggleMenuItem={this.props.toggleMenuItem}>
          <IndexLink to="users" className="fc-navigation-item__sublink">Users</IndexLink>
          <Link to="plugins" className="fc-navigation-item__sublink">Plugins</Link>
        </NavigationItem>
      </li>
    );
  }
}
