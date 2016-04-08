
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectFormInner from '../object-form/object-form-inner';
import ContentBox from '../content-box/content-box';
import Dropdown from '../dropdown/dropdown';

// styles
import styles from './form.css';

export default class CouponForm extends Component {

  get generalAttrs() {
    const toOmit = [
      'qualifier'
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
            Currency
          </label>
        </div>
        <div>
          <Dropdown
            id="promotionSelector"
            name="promotion"
            items={[[3, 'Coupon promo']]}
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

  render() {
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
        </div>
        <div styleName="aside">
        </div>
      </div>
    );
  }

};


