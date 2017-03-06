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
  id?: string,
};

const giftCardClaims = readAction(frn.mkt.giftCard);
const promotionClaims = readAction(frn.mkt.promotion);
const couponClaims = readAction(frn.mkt.coupon);


export default class MarketingEntry extends Component {
  props: Props;

  render() {
    const { claims, routes } = this.props;
    const allClaims = { ...giftCardClaims, ...promotionClaims, ...couponClaims };

    if (!anyPermitted(allClaims, claims)) {
      return <div></div>;
    }

    return (
      <div>
        <h3>MARKETING</h3>
        <li>
          <NavigationItem
            to="gift-cards"
            icon="gift-cards"
            title="Gift Cards"
            routes={routes}
            actualClaims={claims}
            expectedClaims={giftCardClaims} />
        </li>
        <li>
          <NavigationItem
            to="promotions"
            icon="promotions"
            title="Promotions"
            routes={routes}
            actualClaims={claims}
            expectedClaims={promotionClaims} />
        </li>
        <li>
          <NavigationItem
            to="coupons"
            icon="coupons"
            title="Coupons"
            routes={routes}
            actualClaims={claims}
            expectedClaims={couponClaims} />
        </li>
      </div>

    );
  }
}
