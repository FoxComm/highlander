/* @flow */
import React, { Component, Element } from 'react';
import _ from 'lodash';

import NavigationItem from 'components/sidebar/navigation-item';
import { IndexLink, Link } from 'components/link';

type Props = {
  routes: Object,
  collapsed: boolean,
  status: string,
  toggleMenuItem: Function,
};

export default class MarketingEntry extends Component {
  props: Props;

  render(): Element {
    // TODO: Insert logic that will determine what items show.
    return (
      <li>
        <NavigationItem
          to="gift-cards"
          icon="icon-discounts"
          title="Marketing"
          isIndex={true}
          isExpandable={true}
          routes={this.props.routes}
          collapsed={this.props.collapsed}
          status={this.props.status}
          toggleMenuItem={this.props.toggleMenuItem}>
          <IndexLink to="gift-cards" className="fc-navigation-item__sublink">Gift Cards</IndexLink>
          <IndexLink to="promotions" className="fc-navigation-item__sublink">Promotions</IndexLink>
          <IndexLink to="coupons" className="fc-navigation-item__sublink">Coupons</IndexLink>
        </NavigationItem>
      </li>
    );
  }
}
