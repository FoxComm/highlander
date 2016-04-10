
// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ContentBox from '../../content-box/content-box';
import RadioButton from '../../forms/radio-button';
import { PrimaryButton } from '../../common/buttons';
import { Checkbox } from '../../checkbox/checkbox';
import Counter from '../../forms/counter';
import FormField from '../../forms/formfield';

// styles
import styles from './styles.css';

export default class UsageRules extends Component {

  render() {
    return (
      <ContentBox title="Usage Rules">
        <div styleName="form-group">
          <label htmlFor="couponIsExclusive">
            <Checkbox id="couponIsExclusive"/>
            <span>Coupon is exclusive</span>
          </label>
        </div>
        <div styleName="form-group">
          <div className="fc-form-field" styleName="form-group">
            <div>
              <label>Max uses per coupon</label>
            </div>
            <label htmlFor="couponUnlimitedUsage">
              <Checkbox id="couponUnlimitedUsage" />
              <span>Unlimited</span>
            </label>
            <div>
              <Counter id="couponUsesForCode" min={1} />
            </div>
          </div>
          <div styleName="field-comment">
            Maximum times the coupon can be used.
          </div>
        </div>
        <div styleName="form-group">
          <div className="fc-form-field" styleName="form-group">
            <div>
              <label>Max uses per customer</label>
            </div>
            <label htmlFor="couponCustomerUsage">
              <Checkbox id="couponCustomerUsage" />
              <span>Unlimited</span>
            </label>
            <div>
              <Counter id="couponUsesNumberForCustomer" min={1} />
            </div>
          </div>
          <div styleName="field-comment">
            Maximum times the coupon can be used per customer account.
          </div>
        </div>
      </ContentBox>
    );
  }
}
