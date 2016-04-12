
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

  constructor(props, ...args) {
    super(props, ...args);
    this.state = {
      isExclusive: props.isExclusive,
      unlimitedUsagePerCode: props.isUnlimitedPerCode,
      unlimitedUsagePerCustomer: props.unlimitedUsagePerCustomer,
      usesPerCode: props.usesPerCode,
      usesPerCustomer: props.usesPerCustomer,
    };
  }

  componentWillReceiveProps(nextProps) {
    this.state = {
      isExclusive: nextProps.isExclusive,
      unlimitedUsagePerCode: nextProps.isUnlimitedPerCode,
      unlimitedUsagePerCustomer: nextProps.unlimitedUsagePerCustomer,
      usesPerCode: nextProps.usesPerCode,
      usesPerCustomer: nextProps.usesPerCustomer,
    };
  }

  @autobind
  toggleExclusiveness() {
    this.setState({isExclusive: !this.isExclusive});
  }

  @autobind
  toggleUnlimitedUsage() {
    this.setState({unlimitedUsagePerCode: !this.state.unlimitedUsagePerCode});
  }

  @autobind
  toggleUnlimitedCustomerUsage() {
    this.setState({unlimitedUsagePerCustomer: !this.state.unlimitedUsagePerCustomer});
  }

  @autobind
  handleUsesPerCodeChange(value) {
    const checkedValue = value < 1 ? 1 : value;
    this.setState({usesPerCode: checkedValue});
  }

  @autobind
  handleUsesPerCustomerChange(value) {
    console.log(value);
    const checkedValue = value < 1 ? 1 : value;
    this.setState({usesPerCustomer: checkedValue});
  }

  render() {
    console.log(this.props);
    console.log('state');
    console.log(this.state);
    return (
      <ContentBox title="Usage Rules">
        <div styleName="form-group">
          <Checkbox
            id="couponIsExclusive"
            onClick={this.toggleExclusiveness}
            checked={this.state.isExclusive}>
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
                checked={this.state.unlimitedUsagePerCode}>
                Unlimited
              </Checkbox>
            </div>
            <div>
              <Counter
                id="couponUsesForCode"
                value={this.state.usesPerCode}
                disabled={this.state.unlimitedUsagePerCode}
                decreaseAction={() => this.handleUsesPerCodeChange(this.state.usesPerCode - 1)}
                increaseAction={() => this.handleUsesPerCodeChange(this.state.usesPerCode + 1)}
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
                checked={this.state.unlimitedUsagePerCustomer}>
                Unlimited
              </Checkbox>
            </div>
            <div>
              <Counter
                id="couponUsesNumberForCustomer"
                value={this.state.usesPerCustomer}
                disabled={this.state.unlimitedUsagePerCustomer}
                decreaseAction={() => this.handleUsesPerCustomerChange(this.state.usesPerCustomer - 1)}
                increaseAction={() => this.handleUsesPerCustomerChange(this.state.usesPerCustomer + 1)}
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
