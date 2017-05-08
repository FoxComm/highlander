
/* @flow weak */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

import styles from '../object-page/object-details.css';

import ObjectDetails from '../object-page/object-details';
import { FormField } from '../forms';
import RadioButton from '../forms/radio-button';
import SelectCustomerGroups from '../customers-groups/select-groups';
import DiscountAttrs from './discount-attrs';
import offers from './offers';
import qualifiers from './qualifiers';
import Discounts from './discounts';

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
            <label htmlFor="autoApplyRadio" styleName="field-label">Promotion is automatically applied</label>
          </RadioButton>
          <RadioButton id="couponCodeRadio"
            onChange={this.handleApplyTypeChange}
            name="coupon"
            checked={promotion.applyType === 'coupon'}>
            <label htmlFor="couponCodeRadio" styleName="field-label">Promotion requires a coupon code</label>
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
      <FormField className="fc-object-form__field">
        <div>
          <RadioButton id="isExlusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="true"
            checked={isExclusive === true}>
            <label htmlFor="isExlusiveRadio">Promotion is exclusive</label>
          </RadioButton>
          <RadioButton id="notExclusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="false"
            checked={isExclusive === false}>
            <label htmlFor="notExclusiveRadio">Promotion can be used with other promotions</label>
          </RadioButton>
        </div>
      </FormField>
    );
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

  renderDiscountsSection() {
    let qualifier = _.get(this.props.object, 'discounts.0.attributes.qualifier.v');
    let offer = _.get(this.props.object, 'discounts.0.attributes.offer.v');
    return (
      <div>
        <Discounts
          onChangeQualifier={this.handleQualifierChange}
          onChangeOffer={this.handleOfferChange}
          discounts={
            {
              qualifier: {
                ...qualifier
              },
              offer: {
                ...offer
              }
            }
          }
        />
      </div>
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
}
