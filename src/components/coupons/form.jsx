
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectForm from '../object-form/object-form';

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

  get fieldsOptions() {
    return {
      'promotionId': []
    };
  }

  @autobind
  handleChange(form, shadow) {
    const newCoupon = assoc(this.props.coupon,
      ['form', 'attributes'], form,
      ['shadow', 'attributes'], shadow
    );

    this.props.onUpdateCoupon(newCoupon);
  }

  render() {
    const formAttributes = _.get(this.props, 'coupon.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'coupon.shadow.attributes', []);

    return (
      <div styleName="coupon-form">
        <div styleName="main">
          <ObjectForm
            onChange={this.handleChange}
            fieldsToRender={this.generalAttrs}
            fieldsOptions={this.fieldsOptions}
            form={formAttributes}
            shadow={shadowAttributes}
            title="General"
          />
        </div>
        <div styleName="aside">
        </div>
      </div>
    );
  }

};


