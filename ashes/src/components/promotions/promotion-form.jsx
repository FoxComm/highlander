
/* @flow weak */

import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

import styles from '../object-page/object-details.css';

import ObjectDetails from '../object-page/object-details';
import { FormField } from '../forms';
import RadioButton from 'components/core/radio-button';
import SelectCustomerGroups from '../customers-groups/select-groups';
import DiscountAttrs from './discount-attrs';
import offers from './offers';
import qualifiers from './qualifiers';

import { setDiscountAttr } from 'paragons/promotion';
import { setObjectAttr, omitObjectAttr } from 'paragons/object';
import { customerGroups } from 'paragons/object-types';
const layout = require('./layout.json');

export default class PromotionForm extends ObjectDetails {

  layout = layout;

  renderApplyType() {
    const promotion = this.props.object;
    return (
      <FormField
        ref="applyTypeField"
        className="fc-object-form__field"
      >
        <div>
          <RadioButton id="autoApplyRadio"
            onChange={this.handleApplyTypeChange}
            name="auto"
            checked={promotion.applyType === 'auto'}>
            Promotion is automatically applied
          </RadioButton>
          <RadioButton id="couponCodeRadio"
            onChange={this.handleApplyTypeChange}
            name="coupon"
            checked={promotion.applyType === 'coupon'}>
            Promotion requires a coupon code
          </RadioButton>
        </div>
      </FormField>
    );
  }


  get usageRules() {
    return _.get(this.props, 'object.attributes.usageRules.v', {});
  }

  renderUsageRules() {
    const isExclusive = _.get(this.usageRules, 'isExclusive');
    return (
      <FormField
        className="fc-object-form__field"
      >
        <div>
          <RadioButton id="isExlusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="true"
            checked={isExclusive === true}>
            Promotion is exclusive
          </RadioButton>
          <RadioButton id="notExclusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="false"
            checked={isExclusive === false}>
           Promotion can be used with other promotions
          </RadioButton>
        </div>
      </FormField>
    );
  }

  @autobind
  handleQualifierChange(qualifier: Object) {
    const newPromotion = setDiscountAttr(this.props.object,
      'qualifier', qualifier
    );

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleOfferChange(offer: Object) {
    const newPromotion = setDiscountAttr(this.props.object,
      'offer', offer
    );

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleApplyTypeChange({target}: Object) {
    const value = target.getAttribute('name');
    const newPromotion = assoc(this.props.object, 'applyType', value);

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleUsageRulesChange({target}: Object) {
    const value = (target.getAttribute('name') === 'true');
    const newPromotion = setObjectAttr(this.props.object, 'usageRules', {
      t: 'PromoUsageRules',
      v: {
        'isExclusive': value
      }
    });

    this.props.onUpdateObject(newPromotion);
  }

  renderState(): ?Element<*> {
    return super.renderState();
  }

  renderDiscounts() {
    let discountChilds = [];
    const discounts = _.get(this.props.object, 'discounts', []);
    discounts.map((disc,index) => {
      discountChilds.push(<div styleName="sub-title">Qualifier</div>),
      discountChilds.push(<DiscountAttrs
        blockId={'promo-qualifier-block-'+index}
        dropdownId={'promo-qualifier-dd-'+index}
        discount={disc}
        attr="qualifier"
        descriptions={qualifiers}
        onChange={this.handleQualifierChange}
      />);
      discountChilds.push(<div styleName="sub-title">Offer</div>),
      discountChilds.push(<DiscountAttrs
        blockId={'promo-offer-block-'+index}
        dropdownId={'promo-offer-dd-'+index}
        discount={disc}
        attr="offer"
        descriptions={offers}
        onChange={this.handleOfferChange}
      />);
    });
    return (
      <div>
        {discountChilds}
      </div>
    );
  }

  @autobind
  handleQualifyAllChange(isAllQualify) {
    const promotion = this.props.object;
    let newPromotion;
    if (isAllQualify) {
      newPromotion = omitObjectAttr(promotion, 'customerGroupIds');
    } else {
      newPromotion = setObjectAttr(promotion, 'customerGroupIds', customerGroups([]));
    }
    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleQualifierGroupChange(ids){
    const promotion = this.props.object;
    const newPromotion = setObjectAttr(promotion, 'customerGroupIds', customerGroups(ids));
    this.props.onUpdateObject(newPromotion);
  }

  renderCustomers(): Element<*> {
    const promotion = this.props.object;
    return (
      <div styleName="customer-groups">
        <div styleName="sub-title" >Customers</div>
        <SelectCustomerGroups
          parent="Promotions"
          selectedGroupIds={_.get(promotion, 'attributes.customerGroupIds.v', null)}
          qualifyAll={_.get(promotion, 'attributes.customerGroupIds.v', null) == null}
          qualifyAllChange={this.handleQualifyAllChange}
          updateSelectedIds={this.handleQualifierGroupChange}
        />
      </div>
    );
  }
}
