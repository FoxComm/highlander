
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

  get dataProps() {
    const {onChange, ...dataProps} = this.props;
    return dataProps;
  }

  @autobind
  toggleExclusiveness() {
    const newState = {...(this.dataProps), isExclusive: !this.props.isExclusive};
    this.props.onChange(newState);
  }

  @autobind
  toggleUnlimitedUsage() {
    const newState = {...(this.dataProps), isUnlimitedPerCode: !this.props.isUnlimitedPerCode};
    this.props.onChange(newState);
  }

  @autobind
  toggleUnlimitedCustomerUsage() {
    const newState = {...(this.dataProps), isUnlimitedPerCustomer: !this.props.isUnlimitedPerCustomer};
    this.props.onChange(newState);
  }

  @autobind
  handleUsesPerCodeChange(value) {
    const checkedValue = value < 1 ? 1 : value;
    const newState = {...(this.dataProps), usesPerCode: checkedValue};
    this.props.onChange(newState);
  }

  @autobind
  handleUsesPerCustomerChange(value) {
    const checkedValue = value < 1 ? 1 : value;
    const newState = {...(this.dataProps), usesPerCustomer: checkedValue};
    this.props.onChange(newState);
  }

  render() {
    return (
      <ContentBox title="Usage Rules">
        <div styleName="form-group">
          <Checkbox
            id="couponIsExclusive"
            onClick={this.toggleExclusiveness}
            checked={this.props.isExclusive}>
            Coupon is exclusive
          </Checkbox>
        </div>
        <div styleName="form-group">
          <div className="fc-form-field" styleName="form-group">
            <div styleName="form-group-title">
              <label>Max uses per coupon</label>
            </div>
            <div styleName="form-group">
              <Checkbox id="couponUnlimitedUsage"
                onClick={this.toggleUnlimitedUsage}
                checked={this.props.isUnlimitedPerCode}>
                Unlimited
              </Checkbox>
            </div>
            <div>
              <Counter
                id="couponUsesForCode"
                value={this.props.usesPerCode}
                disabled={this.props.isUnlimitedPerCode}
                decreaseAction={() => this.handleUsesPerCodeChange(this.props.usesPerCode - 1)}
                increaseAction={() => this.handleUsesPerCodeChange(this.props.usesPerCode + 1)}
                onChange={({target}) => this.handleUsesPerCodeChange(target.value)}
                min={1}
              />
            </div>
          </div>
          <div styleName="field-comment">
            Maximum times the coupon can be used.
          </div>
        </div>
        <div styleName="form-group">
          <div className="fc-form-field" styleName="form-group">
            <div styleName="form-group-title">
              <label>Max uses per customer</label>
            </div>
            <div styleName="form-group">
              <Checkbox id="couponCustomerUsage"
                onClick={this.toggleUnlimitedCustomerUsage}
                checked={this.props.isUnlimitedPerCustomer}>
                Unlimited
              </Checkbox>
            </div>
            <div>
              <Counter
                id="couponUsesNumberForCustomer"
                value={this.props.usesPerCustomer}
                disabled={this.props.isUnlimitedPerCustomer}
                decreaseAction={() => this.handleUsesPerCustomerChange(this.props.usesPerCustomer - 1)}
                increaseAction={() => this.handleUsesPerCustomerChange(this.props.usesPerCustomer + 1)}
                onChange={({target}) => this.handleUsesPerCustomerChange(target.value)}
                min={1}
              />
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
