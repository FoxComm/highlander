
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import ContentBox from '../../content-box/content-box';
import RadioButton from '../../forms/radio-button';
import { PrimaryButton } from '../../common/buttons';
import { Checkbox } from '../../checkbox/checkbox';
import Counter from '../../forms/counter';

// styles
import styles from './coupon-codes.css';

export default class CouponCodes extends Component {

  constructor(...attrs) {
    super(...attrs);
    this.state = {
      single: false,
      bulk: false,
    };
  }

  get singleCouponFormPart() {
    if (!this.state.single) {
      return null;
    }

    return (
      <div styleName="form-subset">
        <input type="text" />
        <div>
          Coupon codes must be unique and are <i>not</i> case sensative.
        </div>
      </div>
    );
  }

  get bulkCouponFormPart() {
    if (!this.state.bulk) {
      return null;
    }

    return (
      <div styleName="form-subset">
        <div>
          <label htmlFor="codesQuantity">Quantity</label>
          <Counter
            id="codesQuantity"
            min={1}
          />
        </div>
        <div>
          <div>
            <label htmlFor="couponCodePrefix">Code Prefix</label>
          </div>
          <div>
            <input type="text" id="couponCodePrefix" />
          </div>
        </div>
        <div>
          <div>
            <label htmlFor="couponCodeLength">Code Character Length</label>
          </div>
          <div>
            <input type="text" id="couponCodeLength" />
          </div>
          <div styleName="field-comment">
            Excludes prefix
          </div>
        </div>
        <div>
          <label htmlFor="downloadCSVCheckbox">
            <Checkbox id="downloadCSVCheckbox"/>
            <span>Download a CSV file of the coupon codess</span>
          </label>
        </div>
        <div>
          <label htmlFor="emailCSVCheckbox">
            <Checkbox id="emailCSVCheckbox"/>
            <span>Email a CSV file of the coupon codes to other users</span>
          </label>
        </div>
        <PrimaryButton type="button">Generate Codes</PrimaryButton>
      </div>
    );
  }

  @autobind
  handleSingleSelect() {
    this.setState({single: true, bulk: false});
  }

  @autobind
  handleBulkSelect() {
    this.setState({single: false, bulk: true});
  }

  render() {
    return (
      <ContentBox title="Coupon Code">
        <div>
          <RadioButton id="singleCouponCodeRadio"
                       checked={this.state.single}
                       onChange={this.handleSingleSelect} >
            <label htmlFor="singleCouponCodeRadio" styleName="field-label">Single coupon code</label>
          </RadioButton>
        </div>
        {this.singleCouponFormPart}
        <div>
          <RadioButton id="bulkCouponCodeRadio"
                       checked={this.state.bulk}
                       onChange={this.handleBulkSelect} >
            <label htmlFor="bulkCouponCodeRadio" styleName="field-label">Bulk generate coupon codes</label>
          </RadioButton>
        </div>
        {this.bulkCouponFormPart}
      </ContentBox>
    );
  }
}
