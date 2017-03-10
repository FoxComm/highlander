/* @flow weak */

// libs
import _ from 'lodash';
import React, { Element } from 'react';

// components
import SubNav from './sub-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';
import { transitionTo } from 'browserHistory';

// actions
import * as PromotionActions from 'modules/promotions/details';

class PromotionPage extends ObjectPage {
  save(): ?Promise {
  	let isNew = this.isNew;
    let willBePromo = super.save();

    if (willBePromo && isNew) {
        willBePromo.then((data) => {
        	if (data.applyType === 'coupon') {
        		transitionTo('promotion-coupon-new',{promotionId: data.id});
        	}
        });
    }

    return willBePromo;
  }
  subNav(): Element {
    return <SubNav applyType={_.get(this.props, 'details.promotion.applyType')} promotionId={this.entityId} />;
  }
}

export default connectPage('promotion', PromotionActions)(PromotionPage);
