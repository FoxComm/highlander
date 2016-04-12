
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectFormInner from '../object-form/object-form-inner';
import ContentBox from '../content-box/content-box';
import Dropdown from '../dropdown/dropdown';
import RadioButton from '../forms/radio-button';
import CouponCodes from './form/coupon-codes';
import UsageRules from './form/usage-rules';

// styles
import styles from './form.css';

export default class CouponForm extends Component {

  get generalAttrs() {
    const toOmit = [
      'activeFrom',
      'activeTo',
    ];
    const shadow = _.get(this.props, 'coupon.shadow.attributes', []);
    return _(shadow).omit(toOmit).keys().value();
  }

  get promotionSelector() {
    const id = _.get(this.props, 'coupon.promotion');
    return (
      <div>
        <div styleName="field-label">
          <label htmlFor="promotionSelector">
            Promotion
          </label>
        </div>
        <div>
          <Dropdown
            id="promotionSelector"
            styleName="full-width"
            name="promotion"
            items={[[2157, 'Coupon promo']]}
            placeholder="- Select -"
            value={id}
            onChange={(value) => this.handlePromotionChange(value)}
          />
        </div>
        <div styleName="field-comment">
          Only promotions with the Coupon Apply Type can be attached to a coupon.
        </div>
      </div>
    );
  }

  @autobind
  handleChange(form, shadow) {
    const newCoupon = assoc(this.props.coupon,
      ['form', 'attributes'], form,
      ['shadow', 'attributes'], shadow
    );

    this.props.onUpdateCoupon(newCoupon);
  }

  @autobind
  handlePromotionChange(value) {
    const coupon = assoc(this.props.coupon, 'promotion', value);
    this.props.onUpdateCoupon(coupon);
  }

  @autobind
  handleGenerateBulkCodes(prefix, length, quantity) {
    this.props.onGenerateBulkCodes(prefix, length, quantity);
  }

  @autobind
  handleChangeSingleCode(value) {
    this.props.onUpdateCouponCode(value);
  }

  get isNew() {
    return this.props.entity.entityId === 'new';
  }

  get usageRules() {
    return _.get(this.props, 'coupon.form.usageRules', {});
  }

  render() {
    console.log(this.props);
    const formAttributes = _.get(this.props, 'coupon.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'coupon.shadow.attributes', []);

    return (
      <div styleName="coupon-form">
        <div styleName="main">
          <ContentBox title="General">
            <ObjectFormInner
              onChange={this.handleChange}
              fieldsToRender={this.generalAttrs}
              fieldsOptions={this.fieldsOptions}
              form={formAttributes}
              shadow={shadowAttributes}
            />
            {this.promotionSelector}
          </ContentBox>
          <CouponCodes
            onGenerateBulkCodes={this.handleGenerateBulkCodes}
            onChangeSingleCode={this.handleChangeSingleCode}
          />
          <UsageRules {...(this.usageRules)} />
        </div>
        <div styleName="aside">
        </div>
      </div>
    );
  }

};


