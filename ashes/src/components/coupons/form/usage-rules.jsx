/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ContentBox from '../../content-box/content-box';
import { Checkbox } from '../../checkbox/checkbox';
import Counter from '../../forms/counter';

// styles
import styles from './styles.css';

type UsageRuleProps = {
  isExclusive: boolean,
  isUnlimitedPerCode: boolean,
  isUnlimitedPerCustomer: boolean,
  usesPerCode: number,
  usesPerCustomer: number,
  onChange: Function,
}

export default class UsageRules extends Component {

  props: UsageRuleProps;

  static defaultProps = {
    isExclusive: false,
    isUnlimitedPerCode: false,
    isUnlimitedPerCustomer: false,
    usesPerCode: 1,
    usesPerCustomer: 1,
  };

  @autobind
  toggleExclusiveness(): void {
    this.props.onChange('isExclusive', !this.props.isExclusive);
  }

  @autobind
  toggleUnlimitedUsage(): void {
    this.props.onChange('isUnlimitedPerCode', !this.props.isUnlimitedPerCode);
  }

  @autobind
  toggleUnlimitedCustomerUsage(): void {
    this.props.onChange('isUnlimitedPerCustomer', !this.props.isUnlimitedPerCustomer);
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
          <div styleName="field-comment">
            Maximum times the coupon can be used per customer account.
          </div>
        </div>
      </ContentBox>
    );
  }
}
