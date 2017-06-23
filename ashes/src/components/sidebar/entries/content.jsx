/* @flow */

import React from 'react';

import { anyPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import NavigationItem from '../navigation-item';

import styles from './entries.css';

const giftCardClaims = readAction(frn.mkt.giftCard);
const promotionClaims = readAction(frn.mkt.promotion);
const couponClaims = readAction(frn.mkt.coupon);

const MarketingEntry = ({ claims, routes }: TMenuEntry) => {
  const allClaims = { ...giftCardClaims, ...promotionClaims, ...couponClaims };

  if (!anyPermitted(allClaims, claims)) {
    return <div></div>;
  }

  return (
    <div styleName="fc-entries-wrapper">
      <h3>CONTENT</h3>
      <li>
        <NavigationItem
          to="content-types"
          icon="content-types"
          title="Content Types"
          routes={routes}
          actualClaims={claims}
          expectedClaims={giftCardClaims}
        />
      </li>
    </div>

  );
};

export default MarketingEntry;
