
/* @flow weak */

import _ from 'lodash';
import React, { PropTypes, Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

import styles from '../object-page/object-details.css';

import ObjectDetails from '../object-page/object-details';
import { Dropdown } from '../dropdown';
import { FormField } from '../forms';
import SelectCustomerGroups from '../customers-groups/select-groups';
import DiscountAttrs from './discount-attrs';
import offers from './offers';
import qualifiers from './qualifiers';


import { setDiscountAttr } from 'paragons/promotion';
const layout = require('./layout.json');

type State = {
  qualifiedCustomerGroupIds: Array<any>,
};

const SELECT_COUPON_TYPE = [
  ['auto', 'Auto'],
  ['coupon', 'Coupon'],
];

export default class PromotionForm extends ObjectDetails {
  // $FlowFixMe: flow!
  state: State = {
    qualifiedCustomerGroupIds: [], // it's temporary state until qualified customer groups not implemented in backend!
  };
  layout = layout;

  renderApplyType() {
    const promotion = this.props.object;

    return (
      <FormField
        ref="applyTypeField"
        className="fc-object-form__field"
        label="Apply Type"
        getTargetValue={() => promotion.applyType}
        required
      >
        <div>
          <Dropdown
            id="apply-type-dd"
            placeholder="- Select -"
            value={promotion.applyType}
            onChange={this.handleApplyTypeChange}
            items={SELECT_COUPON_TYPE}
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
  handleApplyTypeChange(value: any) {
    const newPromotion = assoc(this.props.object, 'applyType', value);

    this.props.onUpdateObject(newPromotion);
    this.refs.applyTypeField.validate();
  }

  renderState(): ?Element {
    const applyType = this.props.object.applyType;
    if (applyType == 'coupon') {
      return null;
    }

    return super.renderState();
  }

  renderQualifier() {
    const discount = _.get(this.props.object, 'discounts.0', {});

    return [
      <div styleName="sub-title" key="title">Qualifier Type</div>,
      <DiscountAttrs
        blockId="promo-qualifier-block"
        dropdownId="promo-qualifier-dd"
        key="attrs"
        discount={discount}
        attr="qualifier"
        descriptions={qualifiers}
        onChange={this.handleQualifierChange}
      />
    ];
  }

  renderOffer() {
    const discount = _.get(this.props.object, 'discounts.0', {});

    return [
      <div styleName="sub-title" key="title">Offer Type</div>,
      <DiscountAttrs
        blockId="promo-offer-block"
        dropdownId="promo-offer-dd"
        key="attrs"
        discount={discount}
        attr="offer"
        descriptions={offers}
        onChange={this.handleOfferChange}
      />
    ];
  }

  renderCustomers() {
    return (
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
    );
  }
}
