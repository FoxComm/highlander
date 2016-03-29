
/* @flow weak */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './checkout.css';
import textStyles from 'ui/css/input.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { detectCardType, cardMask, cvvLength} from 'lib/payment-cards';

import { Form, FormField } from 'ui/forms';
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import Checkbox from 'ui/checkbox';
import EditableBlock from 'ui/editable-block';
import Autocomplete from 'ui/autocomplete';
import InputMask from 'react-input-mask';
import EditAddress from './edit-address';
import Icon from 'ui/icon';
import ViewAddress from './view-address';

import type { CheckoutBlockProps } from './types';
import * as checkoutActions from 'modules/checkout';
import { AddressKind } from 'modules/checkout';
import type { BillingData } from 'modules/checkout';

let ViewBilling = (props) => {
  const billingData: BillingData = props.billingData;

  const paymentType = detectCardType(billingData.cardNumber);

  const lastFour = billingData.cardNumber && billingData.cardNumber.slice(-4);
  const lastTwoYear = billingData.expYear && billingData.expYear.slice(-2);
  const monthYear = billingData.expMonth || billingData.expYear
    ? <span>{billingData.expMonth}/{lastTwoYear}</span>
    : null;
  const addressInfo = !_.isEmpty(props.billingAddress)
    ? <ViewAddress styleName="billing-address" {...props.billingAddress}/>
    : null;

  return (
    <div>
      {paymentType && <Icon styleName="payment-icon" name={`fc-payment-${paymentType}`} />}
      <div styleName="payment-card-info">
        <span styleName="payment-last-four">{lastFour}</span>
        {monthYear}
      </div>
      {addressInfo}
    </div>
  );
};
ViewBilling = connect(state => state.checkout)(ViewBilling);

function mapStateToProps(state) {
  return {
    data: state.checkout.billingData,
    billingAddressIsSame: state.checkout.billingAddressIsSame,
  };
}

const months = _.range(1, 13, 1).map(x => _.padStart(x.toString(), 2, '0'));
const currentYear = new Date().getFullYear();
const years = _.range(currentYear, currentYear + 10, 1).map(x => x.toString());

/* ::`*/
@connect(mapStateToProps, checkoutActions)
/* ::`*/
class EditBilling extends Component {

  @autobind
  handleSubmit() {
    this.props.continueAction();
  }

  @autobind
  changeFormData({target}) {
    this.props.setBillingData(target.name, target.value);
  }

  @autobind
  changeCVV({target}) {
    const value = target.value.replace(/[^\d]/g, '').substr(0, cvvLength(this.cardType));
    this.props.setBillingData('cvv', value);
  }

  @autobind
  changeCardNumber({target}) {
    const value = target.value.replace(/[^\d]/g, '');

    this.props.setBillingData('cardNumber', value);
  }

  @autobind
  changeMonth(month) {
    this.props.setBillingData('expMonth', month);
  }

  @autobind
  changeYear(year) {
    this.props.setBillingData('expYear', year);
  }

  get billingAddress() {
    const { billingAddressIsSame } = this.props;

    if (billingAddressIsSame) {
      return null;
    }

    return <EditAddress addressKind={AddressKind.BILLING} {...this.props} />;
  }

  get cardType() {
    const { cardNumber } = this.props.data;
    return detectCardType(cardNumber);
  }

  get cardMask() {
    return cardMask(this.cardType);
  }

  @autobind
  validateCardNumber() {
    const mask = this.cardMask.replace(/[^\d]/g, '');
    const { cardNumber } = this.props.data;

    if (mask.length != cardNumber.length) {
      return 'Please enter a valid credit card number';
    }
    return null;
  }

  @autobind
  validateCvvNumber() {
    const { cvv } = this.props.data;

    return cvv.length != cvvLength(this.cardType) ? `Please enter a valid cvv number` : null;
  }

  render() {
    const props = this.props;
    const { data } = props;

    return (
      <Form onSubmit={this.handleSubmit} styleName="checkout-form">
        <FormField styleName="text-field">
          <TextInput required
            name="holderName" placeholder="NAME ON CARD" value={data.holderName} onChange={this.changeFormData}
          />
        </FormField>
        <div styleName="union-fields">
          <FormField styleName="text-field" validator={this.validateCardNumber}>
            <InputMask
              required
              className={textStyles['text-input']}
              maskChar=" "
              type="text"
              mask={this.cardMask}
              name="cardNumber"
              placeholder="CARD NUMBER"
              size="20"
              value={data.cardNumber}
              onChange={this.changeCardNumber}
            />
          </FormField>
          <FormField styleName="text-field" validator={this.validateCvvNumber}>
            <TextInput
              required
              type="number"
              maxLength={cvvLength(this.cardType)}
              placeholder="CVV"
              onChange={this.changeCVV}
              value={data.cvv}
            />
          </FormField>
        </div>
        <div styleName="union-fields">
          <FormField required styleName="text-field" getTargetValue={() => data.expMonth}>
            <Autocomplete
              inputProps={{
                placeholder: 'MONTH',
                type: 'text',
              }}
              compareValues={(value1, value2) => Number(value1) == Number(value2)}
              getItemValue={item => item}
              items={months}
              onSelect={this.changeMonth}
              selectedItem={data.expMonth}
            />
          </FormField>
          <FormField required styleName="text-field" getTargetValue={() => data.expYear}>
            <Autocomplete
              inputProps={{
                placeholder: 'YEAR',
                type: 'number',
              }}
              allowCustomValues
              getItemValue={item => item}
              items={years}
              onSelect={this.changeYear}
              selectedItem={data.expYear}
            />
          </FormField>
        </div>
        <Checkbox
          id="billingAddressIsSame"
          checked={props.billingAddressIsSame}
          onChange={props.toggleSeparateBillingAddress}
        >
          Billing address is same as shipping
        </Checkbox>
        {this.billingAddress}
        <Button styleName="checkout-submit" type="submit">PLACE ORDER</Button>
      </Form>
    );
  }
}

const Billing = (props: CheckoutBlockProps) => {
  const content = props.isEditing
    ? <EditBilling {...props} />
    : <ViewBilling />;

  const deliveryContent = (
    <div styleName="checkout-block-content">
      {content}
    </div>
  );

  return (
    <EditableBlock
      {...props}
      styleName="checkout-block"
      title="BILLING"
      content={deliveryContent}
    />
  );
};

export default Billing;
