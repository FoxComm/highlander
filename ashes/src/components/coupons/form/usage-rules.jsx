/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ContentBox from '../../content-box/content-box';
import RadioButton from '../../forms/radio-button';
import { Checkbox } from '../../checkbox/checkbox';
import Counter from '../../forms/counter';

// styles
import styles from './styles.css';

type UsageRuleProps = {
  isUnlimitedPerCode: boolean,
  isUnlimitedPerCustomer: boolean,
  usesPerCode: number,
  usesPerCustomer: number,
  onChange: Function,
}

export default class UsageRules extends Component {

  props: UsageRuleProps;

  static defaultProps = {
    isUnlimitedPerCode: false,
    isUnlimitedPerCustomer: false,
    usesPerCode: 1,
    usesPerCustomer: 1,
  };

  @autobind
  setUnlimitedUsageTrue(): void {
    this.props.onChange('isUnlimitedPerCode', true);
  }

  @autobind
  setUnlimitedUsageFalse(): void {
    this.props.onChange('isUnlimitedPerCode', false);
  }

  @autobind
  setUnlimitedCustomerUsageTrue(): void {
    this.props.onChange('isUnlimitedPerCustomer', true);
  }

  @autobind
  setUnlimitedCustomerUsageFalse(): void {
    this.props.onChange('isUnlimitedPerCustomer', false);
  }

  @autobind
  handleUsesPerCodeChange(value: number): void {
    const checkedValue = value < 1 ? 1 : value;
    this.props.onChange('usesPerCode', checkedValue);
  }

  @autobind
  handleUsesPerCustomerChange(value: number): void {
    const checkedValue = value < 1 ? 1 : value;
    this.props.onChange('usesPerCustomer', checkedValue);
  }

  shouldComponentUpdate(nextProps: UsageRuleProps): boolean {
    return !_.eq(this.props, nextProps);
  }

  render(): Element {
    return (
      <ContentBox title="Usage Rules">
        <div className="fc-mb20" styleName="form-group">
          <div className="fc-form-field">
            <div styleName="form-group-title">
              <label>Max uses per coupon</label>
            </div>
            <div styleName="form-group">
              <RadioButton id="couponUnlimitedUsageTrue"
                onChange={this.setUnlimitedUsageTrue}
                checked={this.props.isUnlimitedPerCode}>
                <label htmlFor="couponUnlimitedUsageTrue" styleName="field-label">Unlimited</label>
              </RadioButton>    
              <RadioButton id="couponUnlimitedUsageFalse"
                onChange={this.setUnlimitedUsageFalse}
                checked={!this.props.isUnlimitedPerCode}>
                <label htmlFor="couponUnlimitedUsageFalse" styleName="field-label">Limited number</label>
              </RadioButton>  
            </div>
            <div style={{display: this.props.isUnlimitedPerCode ? 'none' : 'block'}}>
              <Counter
                counterId="uses-per-coupon-counter"
                id="couponUsesForCode"
                value={this.props.usesPerCode}
                disabled={this.props.isUnlimitedPerCode}
                decreaseAction={() => this.handleUsesPerCodeChange(this.props.usesPerCode - 1)}
                increaseAction={() => this.handleUsesPerCodeChange(this.props.usesPerCode + 1)}
                onChange={({target}) => this.handleUsesPerCodeChange(parseInt(target.value))}
                min={1}
              />
            </div>
          </div>
          {/*<div styleName="field-comment">
            Maximum times the coupon can be used.
          </div>*/}
        </div>
        <div className="fc-mb20" styleName="form-group">
          <div className="fc-form-field">
            <div styleName="form-group-title">
              <label>Max uses per customer</label>
            </div>
            <div styleName="form-group">
              <RadioButton id="couponUnlimitedCustomerUsageTrue"
                onChange={this.setUnlimitedCustomerUsageTrue}
                checked={this.props.isUnlimitedPerCustomer}>
                <label htmlFor="couponUnlimitedCustomerUsageTrue" styleName="field-label">Unlimited</label>
              </RadioButton>    
              <RadioButton id="couponUnlimitedCustomerUsageFalse"
                onChange={this.setUnlimitedCustomerUsageFalse}
                checked={!this.props.isUnlimitedPerCustomer}>
                <label htmlFor="couponUnlimitedCustomerUsageFalse" styleName="field-label">Limited number</label>
              </RadioButton>  
            </div>
            <div style={{display: this.props.isUnlimitedPerCustomer ? 'none' : 'block'}}>
              <Counter
                counterId="uses-per-customer-counter"
                id="couponUsesNumberForCustomer"
                value={this.props.usesPerCustomer}
                disabled={this.props.isUnlimitedPerCustomer}
                decreaseAction={() => this.handleUsesPerCustomerChange(this.props.usesPerCustomer - 1)}
                increaseAction={() => this.handleUsesPerCustomerChange(this.props.usesPerCustomer + 1)}
                onChange={({target}) => this.handleUsesPerCustomerChange(parseInt(target.value))}
                min={1}
              />
            </div>
          </div>
          {/*<div styleName="field-comment">
            Maximum times the coupon can be used per customer account.
          </div>*/}
        </div>
      </ContentBox>
    );
  }
}
