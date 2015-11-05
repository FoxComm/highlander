'use strict';

import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import { Checkbox } from '../checkbox/checkbox';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';

export default class NewCreditCardBox extends React.Component {

  render() {
    return (
      <li className="fc-card-container fc-credit-cards fc-credit-cards-new">
        <Form className="fc-customer-credit-card-form fc-form-vertical">
          <header>
            New Credit Card
          </header>
          <div>
            <ul className="fc-credit-card-form-fields">
              <li className="fc-credit-card-form-line">
                <label className="fc-credit-card-default-checkbox">
                  <Checkbox defaultChecked={ false } />
                  <span>Default Card</span>
                </label>
              </li>
              <li className="fc-credit-card-form-line">
                <FormField label="Name on Card" validator="ascii">
                  <input id="nameCardFormField"
                         className="fc-customer-form-input"
                         name="name"
                         maxLength="255"
                         type="text"
                         required />
                </FormField>
              </li>
              <li className="fc-credit-card-form-line">
                <div className="fc-grid">
                  <div className="fc-col-md-3-4">
                    <FormField label="Card Number" validator="ascii">
                      <input id="numberCardFormField"
                             className="fc-customer-form-input"
                             name="name"
                             maxLength="255"
                             type="text"
                             required />
                    </FormField>
                  </div>
                  <div className="fc-col-md-1-4">
                    <FormField label="CVV" validator="ascii">
                      <input id="cvvCardFormField"
                             className="fc-customer-form-input"
                             name="name"
                             maxLength="255"
                             type="text"
                             required />
                    </FormField>
                  </div>
                </div>
              </li>
              <li className="fc-credit-card-form-line">
                <FormField label="Expiration Date">
                  <div className="fc-grid">
                    <div className="fc-col-md-1-2">
                      <Dropdown name="" items={[]} placeholder="Month" value={null}/>
                    </div>
                    <div className="fc-col-md-1-2">
                      <Dropdown name="" items={[]} placeholder="Year" value={null}/>
                    </div>
                  </div>
                </FormField>
              </li>
              <li className="fc-credit-card-form-line">
                <div>
                  <label>
                    Billing Address
                  </label>
                  <input type="hidden" name="billingAddressId" id="billingAddressIdCardFormField" />
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
