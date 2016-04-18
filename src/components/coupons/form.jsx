
/* @flow weak */

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
import { FormField, Form } from '../forms';
import Tags from '../tags/tags';

// styles
import styles from './form.css';

type Entity = {
  entityId: number|string,
};

type CouponFormProps = {
  promotionError: bool,
  coupon: Object,
  onUpdateCoupon: Function,
  onGenerateBulkCodes: Function,
  onUpdateCouponCode: Function,
  entity: Entity,
};

export default class CouponForm extends Component {

  static props: CouponFormProps;

  get generalAttrs() {
    const toOmit = [
      'activeFrom',
      'activeTo',
      'usageRules',
      'tags',
    ];
    const shadow = _.get(this.props, 'coupon.shadow.attributes', []);
    return _(shadow).omit(toOmit).keys().value();
  }

  get promotionSelector() {
    const id = _.get(this.props, 'coupon.promotion');
    const promotionsToSelect = _.get(this.props, 'selectedPromotions', []).map((promo) => {
      return [promo.id, `${promo.name} - ${promo.id}`];
    });
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
            items={promotionsToSelect}
            placeholder="- Select -"
            value={id}
            onChange={(value) => this.handlePromotionChange(value)}
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

  @autobind
  handleUsageRulesChange(field, value) {
    const newCoupon = assoc(this.props.coupon, ['form', 'usageRules', field], value);
    this.props.onUpdateCoupon(newCoupon);
  }

  get isNew() {
    return this.props.entity.entityId === 'new';
  }

  get usageRules() {
    return _.get(this.props, 'coupon.form.usageRules', {});
  }

  render() {
    const formAttributes = _.get(this.props, 'coupon.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'coupon.shadow.attributes', []);

    return (
      <Form styleName="coupon-form">
        <div styleName="main">
          <ContentBox title="General">
            <ObjectFormInner
              onChange={this.handleChange}
              fieldsToRender={this.generalAttrs}
              form={formAttributes}
              shadow={shadowAttributes}
            />
            {this.promotionSelector}
          </ContentBox>
          <CouponCodes
            onGenerateBulkCodes={this.handleGenerateBulkCodes}
            onChangeSingleCode={this.handleChangeSingleCode}
          />
          <UsageRules {...(this.usageRules)} onChange={this.handleUsageRulesChange}/>
        </div>
        <div styleName="aside">
          <Tags
            form={formAttributes}
            shadow={shadowAttributes}
            onChange={this.handleChange}
          />
        </div>
      </Form>
    );
  }

};


