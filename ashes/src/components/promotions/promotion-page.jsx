/* @flow weak */

// libs
import _ from 'lodash';
import React, { Element } from 'react';

// components
import SubNav from './sub-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';
import { transitionTo } from 'browserHistory';
import qualifiers from './qualifiers';
import offers from './offers';

// actions
import * as PromotionActions from 'modules/promotions/details';

class PromotionPage extends ObjectPage {

  getDiscountChildErrors(discountChild, discountChildTypes) {
    const errors = [];
    const { object } = this.state;
    const discountChildValue = _.get(object, `discounts[0].attributes.${discountChild}.v`, {});
    const key = _.keys(discountChildValue)[0];

    _.forEach(discountChildValue[key], (v,k) => {

      const { validate } = _.find(discountChildTypes, (dc) => dc.type == key);
      const validator = _.get(validate, `${k}.validate`, (v) => true);

      if (!validator(v)) errors.push(validate[k].error);

    })
    return errors;
  }

  setClientSideErrors(errors) {
    this.setState({
      clientSideErrors: errors,
    })
  }

  save(): ?Promise<*> {
    this.clearClientSideErrors();
    const errors = [
      ...this.getDiscountChildErrors('qualifier', qualifiers),
      ...this.getDiscountChildErrors('offer', offers),
    ];
    if (errors.length) {
      this.setClientSideErrors(errors);
      return;
    }
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

  subNav(): Element<*> {
    return <SubNav applyType={_.get(this.props, 'details.promotion.applyType')} promotionId={this.entityId} />;
  }
}

export default connectPage('promotion', PromotionActions)(PromotionPage);
