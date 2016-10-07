/* @flow */
import React, { Component, Element } from 'react';

import { anyPermitted, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from 'components/sidebar/navigation-item';
import { IndexLink, Link } from 'components/link';

type Props = {
  routes: Object,
  collapsed: boolean,
  status: string,
  toggleMenuItem: Function,
};

const giftCardClaims = readAction(frn.mkt.giftCard);
const promotionClaims = readAction(frn.mkt.promotion);
const couponClaims = readAction(frn.mkt.coupon);


export default class MarketingEntry extends Component {
  props: Props;

  render(): Element {
    const { claims, collapsed, routes, status, toggleMenuItem } = this.props;
    const allClaims = { ...giftCardClaims, ...promotionClaims, ...couponClaims };

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <li>
        <NavigationItem
          to="gift-cards"
          icon="icon-discounts"
          title="Marketing"
          isIndex={true}
          isExpandable={true}
          routes={routes}
          collapsed={collapsed}
          status={status}
          toggleMenuItem={toggleMenuItem}>
          <IndexLink
            to="gift-cards"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={giftCardClaims}>
            Gift Cards
          </IndexLink>
          <IndexLink
            to="promotions"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={promotionClaims}>
            Promotions
          </IndexLink>
          <IndexLink
            to="coupons"
            className="fc-navigation-item__sublink"
            actualClaims={claims}
            expectedClaims={couponClaims}>
            Coupons
          </IndexLink>
        </NavigationItem>
      </li>
    );
  }
}
