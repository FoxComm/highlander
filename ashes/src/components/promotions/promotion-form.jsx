
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
import ElasticQueryGenerator from 'components/query-builder/elastic-query-generator';

import { setDiscountAttr } from 'paragons/promotion';
const layout = require('./layout.json');

type State = {
  qualifiedCustomerGroupIds: Array<any>,
};

export default class PromotionForm extends ObjectDetails {
  // $FlowFixMe: flow!
  state: State = {
    qualifyAll: true,
    qualifiedCustomerGroupIds: [], // it's temporary state until qualified customer groups not implemented in backend!
  };
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
    const applyType = this.props.object.applyType;
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
    const arr = isAllQualify ? [] : promotion.qualifiedCustomerGroupIds;
    const newPromotion1 = assoc(promotion, 'qualifyAll', isAllQualify);
    const newPromotion2 = assoc(newPromotion1, 'qualifiedCustomerGroupIds', arr);
    this.props.onUpdateObject(newPromotion2);
  }

  @autobind
  handleQulifierGroupChange(ids){
    const promotion = this.props.object;
    const newPromotion = assoc(promotion, 'qualifiedCustomerGroupIds', ids);
    this.props.onUpdateObject(newPromotion);
  }

  renderCustomers() {
    const promotion = this.props.object;
    return (
      <div styleName="customer-groups">
        <div styleName="sub-title" >Customers</div>
        <SelectCustomerGroups
          parent="Promotions"
          selectedGroupIds={promotion.qualifiedCustomerGroupIds}
          qualifyAll={promotion.qualifyAll}
          qualifyAllChange={this.handleQualifyAllChange}
          updateSelectedIds={this.handleQulifierGroupChange}
        />
      </div>
    );
  }

  renderDiscountsSection() {
    let qualifier = _.get(this.props.object, 'discounts.0.attributes.qualifier1.v', {
        discountType: 'order',
        qualifierType: 'noQualifier',
        widgetValue: 0,
        exGiftCardQual: true
    });
    let offer = _.get(this.props.object, 'discounts.0.attributes.offer1.v', {
        offerType: 'orderPercentOff',
        exGiftCardOffer: true
    });
    return (<div>
        <Discounts
          onChangeQualifier={this.handleQualifierChange1}
          onChangeOffer={this.handleOfferChange1}
          discounts={{
            qualifier: {
              ...qualifier
            },
            offer: {
              ...offer
            }
        }}/>
      </div>);
  }


  @autobind
  handleQualifierChange1(qualifier: Object) {
    const newPromotion = setDiscountAttr(this.props.object,
      'qualifier1', qualifier
    );
    this.props.onUpdateObject(newPromotion);
  }
  @autobind
  handleOfferChange1(offer: Object) {
    const newPromotion = setDiscountAttr(this.props.object,
      'offer1', offer
    );

    this.props.onUpdateObject(newPromotion);
  }

}
