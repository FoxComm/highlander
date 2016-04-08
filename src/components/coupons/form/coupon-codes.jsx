
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import ContentBox from '../../content-box/content-box';
import RadioButton from '../../forms/radio-button';

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
        <RadioButton id="singleCouponCodeRadio"
                     checked={this.state.single}
                     onChange={this.handleSingleSelect} >
          <label htmlFor="singleCouponCodeRadio" styleName="field-label">Single coupon code</label>
        </RadioButton>
        <RadioButton id="bulkCouponCodeRadio"
                     checked={this.state.bulk}
                     onChange={this.handleBulkSelect} >
          <label htmlFor="bulkCouponCodeRadio" styleName="field-label">Bulk generate coupon codes</label>
        </RadioButton>
      </ContentBox>
    );
  }
}
