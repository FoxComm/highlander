
/* @flow weak */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './checkout.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import { Form, FormField } from 'ui/forms';
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import EditableBlock from 'ui/editable-block';
import Autocomplete from 'ui/autocomplete';

import type { CheckoutBlockProps } from './types';
import * as checkoutActions from 'modules/checkout';

function mapStateToProps(state) {
  return {
    data: state.checkout.billingData,
  };
}

const months = _.range(1, 12, 1).map(x => x.toString());
const currentYear = new Date().getFullYear();
const years = _.range(currentYear, currentYear + 18, 1).map(x => x.toString());

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
  changeMonth(month) {
    this.props.setBillingData('month', month);
  }

  @autobind
  changeYear(year) {
    this.props.setBillingData('year', year);
  }

  render() {
    const { data } = this.props;
    return (
      <Form onSubmit={this.handleSubmit} styleName="checkout-form">
        <FormField styleName="text-field">
          <TextInput required
            name="cardName" placeholder="NAME ON CARD" value={data.cardName} onChange={this.changeFormData}
          />
        </FormField>
        <div styleName="union-fields">
          <FormField styleName="text-field">
            <TextInput
              required
              name="cardNumber"
              placeholder="CARD NUMBER"
              value={data.cardNumber}
              onChange={this.changeFormData}
            />
          </FormField>
          <FormField styleName="text-field">
            <TextInput
              required
              type="number"
              maxLength="3"
              placeholder="CVV"
              onChange={this.changeFormData}
              value={data.zip}
            />
          </FormField>
        </div>
        <div styleName="union-fields">
          <FormField styleName="text-field">
            <Autocomplete
              inputProps={{
                placeholder: 'MONTH',
              }}
              getItemValue={item => item}
              items={months}
              onSelect={this.changeMonth}
              selectedItem={data.month}
            />
          </FormField>
          <FormField styleName="text-field">
            <Autocomplete
              inputProps={{
                placeholder: 'YEAR',
              }}
              getItemValue={item => item}
              items={years}
              onSelect={this.changeYear}
              selectedItem={data.year}
            />
          </FormField>
        </div>
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
