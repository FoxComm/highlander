'use strict';

import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import { CheckBox } from '../checkbox/checkbox';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';

export default class NewCreditCardBox extends React.Component {

  render() {
    return (
      <li className="fc-card-container fc-credit-cards fc-credit-cards-new">
        <Form className="fc-customer-credit-card-form fc-form-vertical">
          <div>
            New Credit Card
          </div>
          <div>
            <ul className="fc-credit-card-form-fields">
              <li>
                <FormField label="Name on Card" validator="ascii">
                  <input id="nameCardFormField"
                         className="fc-customer-form-input"
                         name="name"
                         maxLength="255"
                         type="text"
                         required />
                </FormField>
              </li>
              <li>
                <FormField label="Card Number" validator="ascii">
                  <input id="numberCardFormField"
                         className="fc-customer-form-input"
                         name="name"
                         maxLength="255"
                         type="text"
                         required />
                </FormField>
                <FormField label="CVV" validator="ascii">
                  <input id="cvvCardFormField"
                         className="fc-customer-form-input"
                         name="name"
                         maxLength="255"
                         type="text"
                         required />
                </FormField>
                <FormField label="Expiration Date">
                  <Dropdown name="" items={[]} placeholder="Month" value={null}/>
                  <Dropdown name="" items={[]} placeholder="Year" value={null}/>
                </FormField>
                <div>
                  Billing Address
                </div>
              </li>
            </ul>
          </div>
          <div>
            <a className="fc-btn-link" onClick={ this.props.onCancel }>Cancel</a>
            <PrimaryButton type="submit">Save Customer</PrimaryButton>
          </div>
        </Form>
      </li>
    );
  }
}
