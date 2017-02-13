
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
import AddDiscount from './add-discount';
import offers from './offers';
import qualifiers from './qualifiers';


import { setDiscountAttr } from 'paragons/promotion';
const layout = require('./layout.json');

type State = {
  qualifiedCustomerGroupIds: Array<any>,
};

export default class PromotionForm extends ObjectDetails {
  // $FlowFixMe: flow!
  state: State = {
    qualifiedCustomerGroupIds: [], // it's temporary state until qualified customer groups not implemented in backend!
  };
  layout = layout;

  renderApplyType() { 
    const promotion = this.props.object;
    if(typeof promotion.applyType === "undefined"){ // TO BE REMOVED WHEN applyType WILL BE SET TO "auto" BY DEFAULT
      const newPromotion = assoc(this.props.object, 'applyType', "auto");
      this.props.onUpdateObject(newPromotion);
    }

    return (
      <FormField
        ref="applyTypeField"
        className="fc-object-form__field"
      >
        <div>
          <RadioButton id="autoApplyRadio"
            onChange={this.handleApplyTypeChange}
            name="auto"
            checked={promotion.applyType === "auto"}>
            <label htmlFor="autoApplyRadio" styleName="field-label">Promotion is automatically applied</label>
          </RadioButton>    
          <RadioButton id="couponCodeRadio"
            onChange={this.handleApplyTypeChange}
            name="coupon"
            checked={promotion.applyType === "coupon"}>
            <label htmlFor="couponCodeRadio" styleName="field-label">Promotion requires a coupon code</label>
          </RadioButton>  
        </div>
      </FormField>
    );
  }

  renderUsageRules() { 
    const promotion = this.props.object;
    if(typeof promotion.isExclusive === "undefined"){ // TO BE REMOVED WHEN isExclusive WILL BE SET TO true BY DEFAULT
      const newPromotion = assoc(this.props.object, 'isExclusive', true);
      this.props.onUpdateObject(newPromotion);
    }

    return (
      <FormField
        ref="usageRuleseField"
        className="fc-object-form__field"
      >
        <div>
          <RadioButton id="isExlusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="true"
            checked={promotion.isExclusive === true}>
            <label htmlFor="isExlusiveRadio" styleName="field-label">Promotion is exclusive</label>
          </RadioButton>    
          <RadioButton id="notExclusiveRadio"
            onChange={this.handleUsageRulesChange}
            name="false"
            checked={promotion.isExclusive === false}>
            <label htmlFor="notExclusiveRadio" styleName="field-label">Promotion can be used with other promotions</label>
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
    const value = (target.getAttribute('name') === "true");
    const newPromotion = assoc(this.props.object, 'isExclusive', value);

    this.props.onUpdateObject(newPromotion);
  }

  renderState(): ?Element {

    console.log(this.props);
    const applyType = this.props.object.applyType;
    return super.renderState();
  }

  renderDiscounts() {
    let discountChilds = [];
    const discounts = _.get(this.props.object, 'discounts', []);
    discounts.map(disc => {
        discountChilds.push(<div styleName="sub-title">Qualifier</div>),
        discountChilds.push(<DiscountAttrs 
          blockId="promo-qualifier-block"
          dropdownId="promo-qualifier-dd"
          discount={disc}
          attr="qualifier"
          descriptions={qualifiers}
          onChange={this.handleQualifierChange}
        />);
        discountChilds.push(<div styleName="sub-title">Offer</div>),
        discountChilds.push(<DiscountAttrs 
          blockId="promo-offer-block"
          dropdownId="promo-offer-dd"
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

  renderAddDiscount() {
    return (
      <AddDiscount/>
    );
  }

  renderCustomers() {
    return (
      <div>  
        <div styleName="sub-title" >Customers</div>
        <SelectCustomerGroups
          parent="Promotions"
          selectedGroupIds={this.state.qualifiedCustomerGroupIds}
          onSelect={(ids) => {
            // $FlowFixMe: WTF!
                  this.setState({
                    qualifiedCustomerGroupIds: ids,
                  });
                }}
        />
      </div>
    );
  }
}
