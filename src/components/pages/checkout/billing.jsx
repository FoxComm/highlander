
/* @flow weak */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './checkout.css';
import textStyles from 'ui/css/input.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import { Form, FormField } from 'ui/forms';
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import Checkbox from 'ui/checkbox';
import EditableBlock from 'ui/editable-block';
import Autocomplete from 'ui/autocomplete';
import MaskedInput from 'react-maskedinput';
import EditAddress from './edit-address';

import type { CheckoutBlockProps } from './types';
import * as checkoutActions from 'modules/checkout';
import { AddressKind } from 'modules/checkout';

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

  }

  @autobind
  changeFormData({target}) {
    this.props.setBillingData(target.name, target.value);
  }

  @autobind
  changeCVV({target}) {
    const value = target.value.replace(/[^\d]/g, '').substr(0, 3);
    this.props.setBillingData('cvv', value);
  }

  @autobind
  changeCardNumber({target}) {
    const value = target.value.replace(/[^\d]/g, '');

    this.props.setBillingData('cardNumber', value);
  }

  @autobind
  changeMonth(month) {
    this.props.setBillingData('month', month);
  }

  @autobind
  changeYear(year) {
    this.props.setBillingData('year', year);
  }

  get billingAddress() {
    const { billingAddressIsSame } = this.props;

    if (billingAddressIsSame) {
      return null;
    }

    return <EditAddress addressKind={AddressKind.billing} {...this.props} />;
  }

  render() {
    const props = this.props;
    const { data } = props;

    return (
      <Form onSubmit={this.handleSubmit} styleName="checkout-form">
        <FormField styleName="text-field">
          <TextInput required
            name="cardName" placeholder="NAME ON CARD" value={data.cardName} onChange={this.changeFormData}
          />
        </FormField>
        <div styleName="union-fields">
          <FormField styleName="text-field" validator="cardNumber">
            <MaskedInput
              required
              className={textStyles['text-input']}
              placeholderChar=" "
              type="text"
              mask="1111 1111 1111 1111"
              name="cardNumber"
              placeholder="CARD NUMBER"
              size="20"
              value={data.cardNumber}
              onChange={this.changeCardNumber}
            />
          </FormField>
          <FormField styleName="text-field" validator="cvv">
            <TextInput
              required
              type="number"
              maxLength="3"
              placeholder="CVV"
              onChange={this.changeCVV}
              value={data.cvv}
            />
          </FormField>
        </div>
        <div styleName="union-fields">
          <FormField required styleName="text-field">
            <Autocomplete
              inputProps={{
                placeholder: 'MONTH',
                type: 'text',
              }}
              compareValues={(value1, value2) => Number(value1) == Number(value2)}
              getItemValue={item => item}
              items={months}
              onSelect={this.changeMonth}
              selectedItem={data.month}
            />
          </FormField>
          <FormField required styleName="text-field">
            <Autocomplete
              inputProps={{
                placeholder: 'YEAR',
                type: 'number',
              }}
              allowCustomValues
              getItemValue={item => item}
              items={years}
              onSelect={this.changeYear}
              selectedItem={data.year}
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
  const deliveryContent = (
    <div styleName="checkout-block-content">
      <EditBilling {...props} />
    </div>
  );

  return (
    <EditableBlock
      styleName="checkout-block"
      title="BILLING"
      isEditing={props.isEditing}
      collapsed={props.collapsed}
      editAction={props.editAction}
      content={deliveryContent}
    />
  );
};

export default Billing;
