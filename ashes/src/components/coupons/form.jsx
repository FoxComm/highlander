
/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import { searchCouponPromotions } from 'elastic/promotions';

// components
import DropdownSearch from '../dropdown/dropdown-search';
import DropdownItem from '../dropdown/dropdownItem';
import CouponCodes from './form/coupon-codes';
import UsageRules from './form/usage-rules';
import ObjectDetails from '../object-page/object-details';

import type { DetailsProps } from '../object-page/object-details';

// styles
import styles from './form.css';

type CouponFormProps = {
  promotionError: boolean,
  codeGeneration: Object,
  onUpdateCoupon: Function,
  onGenerateBulkCodes: Function,
  onUpdateCouponCode: Function,
  fetchPromotions: Function,
  createCoupon: Function,
}

const layout = require('./layout.json');

export default class CouponForm extends ObjectDetails {
  props: CouponFormProps & DetailsProps;
  layout = layout;

  handlePromotionSearch(token: string) {
    return searchCouponPromotions(token).then((result) => {
      return result.result;
    });
  }

  @autobind
  renderPromotionOption(promotion) {
    return (
      <DropdownItem value={promotion.id} key={`${promotion.id}-${promotion.promotionName}`}>
        <span>{ promotion.promotionName }</span>
        <span styleName="text-gray">
          &nbsp;<span className="fc-icon icon-dot"></span>&nbsp;ID: { promotion.id }
        </span>
      </DropdownItem>
    );
  }

  get promotionSelector() {
    const id = _.get(this.props, 'object.promotion');
    return (
      <div>
        <div styleName="field-label">
          <label htmlFor="promotionSelector">
            Promotion
          </label>
        </div>
        <div>
          <DropdownSearch
            id="promotionSelector"
            styleName="full-width"
            name="promotion"
            placeholder="- Select -"
            value={id}
            onChange={(value) => this.handlePromotionChange(value)}
            fetchOptions={this.handlePromotionSearch}
            renderOption={this.renderPromotionOption}
            searchbarPlaceholder="Promotion name or storefront name"
          />
        </div>
        { this.props.promotionError && (<div className="fc-form-field-error">
          Promotion must be selected from the list above.
        </div>) }
        <div styleName="field-comment">
          Only promotions with the Coupon Apply Type can be attached to a coupon.
        </div>
      </div>
    );
  }

  @autobind
  handlePromotionChange(value) {
    const coupon = assoc(this.props.object, 'promotion', value);
    this.props.onUpdateObject(coupon);
  }

  @autobind
  handleUsageRulesChange(field, value) {
    const newCoupon = assoc(this.props.object, ['attributes', 'usageRules', 'v', field], value);
    this.props.onUpdateObject(newCoupon);
  }

  get usageRules() {
    return _.get(this.props, 'object.attributes.usageRules.v', {});
  }

  renderPromotionsSelector() {
    return this.promotionSelector;
  }

  renderCouponCodes() {
    return (
      <CouponCodes
        createCoupon={this.props.createCoupon}
        promotionId={this.props.object.promotion}
        codeGeneration={this.props.codeGeneration}
        isNew={this.props.isNew}
      />
    );
  }

  renderUsageRules() {
    return (
      <UsageRules {...(this.usageRules)} onChange={this.handleUsageRulesChange} />
    );
  }
};


