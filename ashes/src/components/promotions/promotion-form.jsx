
/* @flow weak */

import _ from 'lodash';
import React, { PropTypes, Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

import styles from '../object-page/object-details.css';

import ObjectDetails from '../object-page/object-details';
import { Dropdown } from '../dropdown';
import { FormField } from '../forms';
import RadioButton from '../forms/radio-button';
import SelectCustomerGroups from '../customers-groups/select-groups';
import DiscountAttrs from './discount-attrs';
import offers from './offers';
import qualifiers from './qualifiers';
import Discounts from './discounts';


import { setDiscountAttr } from 'paragons/promotion';
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

  renderUsageRules() {
    const promotion = this.props.object;
    return (
      <FormField
        className="fc-object-form__field"
      >
        <div>
          <RadioButton id="isExlusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="true"
            checked={promotion.isExclusive === true}>
            <label htmlFor="isExlusiveRadio">Promotion is exclusive</label>
          </RadioButton>
          <RadioButton id="notExclusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="false"
            checked={promotion.isExclusive === false}>
            <label htmlFor="notExclusiveRadio">Promotion can be used with other promotions</label>
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
    const newPromotion = assoc(this.props.object, 'isExclusive', value);

    this.props.onUpdateObject(newPromotion);
  }

  renderState(): ?Element<*> {
    return super.renderState();
  }

  renderDiscounts() {
    let discountChilds = [];
    const discounts = _.get(this.props.object, 'discounts', []);
    discounts.map((disc,index) => {
        discountChilds.push(<div key={'qtitle-'+index} styleName="sub-title">Qualifier</div>),
        discountChilds.push(<DiscountAttrs
          key={'qual-'+index}
          blockId={'promo-qualifier-block-'+index}
          dropdownId={'promo-qualifier-dd-'+index}
          discount={disc}
          attr="qualifier"
          descriptions={qualifiers}
          onChange={this.handleQualifierChange}
        />);
        discountChilds.push(<div key={'otitle-'+index} styleName="sub-title">Offer</div>),
        discountChilds.push(<DiscountAttrs
          key={'offer-'+index}
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
    const arr = isAllQualify ? null : [];
    const newPromotion = assoc(promotion, 'customerGroupIds', arr);
    this.props.onUpdateObject(newPromotion);
  }

  @autobind
  handleQulifierGroupChange(ids){
    const promotion = this.props.object;
    const newPromotion = assoc(promotion, 'customerGroupIds', ids);
    this.props.onUpdateObject(newPromotion);
  }

  renderCustomers() {
    const promotion = this.props.object;
    return (
      <div styleName="customer-groups">
        <div styleName="sub-title" >Customers</div>
        <SelectCustomerGroups
          parent="Promotions"
          selectedGroupIds={promotion.customerGroupIds}
          qualifyAll={promotion.customerGroupIds == null}
          qualifyAllChange={this.handleQualifyAllChange}
          updateSelectedIds={this.handleQulifierGroupChange}
        />
      </div>
    );
  }
}
