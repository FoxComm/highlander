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
          <RadioButton
            id="autoApplyRadio"
            name="auto"
            label="Promotion is automatically applied"
            onChange={this.handleApplyTypeChange}
            checked={promotion.applyType === 'auto'}
          />
          <RadioButton
            id="couponCodeRadio"
            name="coupon"
            label="Promotion requires a coupon code"
            onChange={this.handleApplyTypeChange}
            checked={promotion.applyType === 'coupon'}
          />
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
          <RadioButton
            id="isExlusiveRadio"
            name="true"
            label="Promotion is exclusive"
            onChange={this.handleUsageRulesChange}
            checked={isExclusive === true}
          />
          <RadioButton
            id="notExclusiveRadio"
            name="false"
            label="Promotion can be used with other promotions"
            onChange={this.handleUsageRulesChange}
            checked={isExclusive === false}
          />
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
  handleApplyTypeChange({ target }: Object) {
    const value = target.getAttribute('name');
    const newPromotion = assoc(this.props.object, 'applyType', value);

    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleUsageRulesChange({ target }: Object) {
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
    const discounts = _.get(this.props.object, 'discounts', []);
    const discountChildren = discounts.reduce ((acc, disc, index) => {
      const makeKey = prefix => `${prefix}-${disc.id || index}`;
      return [
        ...acc,
        <div key={makeKey('qualifier')} styleName="sub-title">Qualifier</div>,
        <DiscountAttrs
          key={makeKey('qualifier-attrs')}
          blockId={'promo-qualifier-block-'+index}
          dropdownId={'promo-qualifier-dd-'+index}
          discount={disc}
          attr="qualifier"
          descriptions={qualifiers}
          onChange={this.handleQualifierChange}
        />,
        <div key={makeKey('offer')} styleName="sub-title">Offer</div>,
        <DiscountAttrs
          key={makeKey('offer-attrs')}
          blockId={'promo-offer-block-'+index}
          dropdownId={'promo-offer-dd-'+index}
          discount={disc}
          attr="offer"
          descriptions={offers}
          onChange={this.handleOfferChange}
        />
      ];
    }, []);

    return (
      <div>
        {discountChildren}
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
  handleQualifierGroupChange(ids) {
    const promotion = this.props.object;
    const newPromotion = setObjectAttr(promotion, 'customerGroupIds', customerGroups(ids));
    this.props.onUpdateObject(newPromotion);
  }

  renderCustomers(): Element<*> {
    const promotion = this.props.object;
    return (
      <div styleName="customer-groups">
        <div styleName="sub-title">Customers</div>
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
