'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import { Checkbox } from '../checkbox/checkbox';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';

export default class NewCreditCardBox extends React.Component {

  get monthList() {
    return {
      1: '01 - January',
      2: '02 - February',
      3: '03 - March',
      4: '04 - April',
      5: '05 - May',
      6: '06 - June',
      7: '07 - July',
      8: '08 - August',
      9: '09 - September',
      10: '10 - October',
      11: '11 - November',
      12: '12 - December'
    };
  }

  get expirationYears() {
    let years = {};
    const current = new Date().getFullYear();
    _.each(_.range(20), (inc) => {
      let year = (current + inc);
      years[year] = year;
    });
    return years;
  }

  render() {
    return (
      <li className="fc-card-container fc-credit-cards fc-credit-cards-new">
        <Form className="fc-customer-credit-card-form fc-form-vertical"
              onChange={ this.props.onChange }>
          <header>
            New Credit Card
          </header>
          <div>
            <ul className="fc-credit-card-form-fields">
              <li className="fc-credit-card-form-line">
                <label className="fc-credit-card-default-checkbox">
                  <Checkbox defaultChecked={ false } name="isDefault" />
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
                             name="cardNumber"
                             maxLength="255"
                             type="text"
                             required />
                    </FormField>
                  </div>
                  <div className="fc-col-md-1-4">
                    <FormField label="CVV" validator="ascii">
                      <input id="cvvCardFormField"
                             className="fc-customer-form-input"
                             name="cvv"
                             maxLength="255"
                             type="text"
                             required />
                    </FormField>
                  </div>
                </div>
              </li>
              <li className="fc-credit-card-form-line">
                <label>Expiration Date</label>
                <div className="fc-grid">
                  <div className="fc-col-md-1-2">
                    <Dropdown name="expitationMonth" items={this.monthList} placeholder="Month" value={null}/>
                  </div>
                  <div className="fc-col-md-1-2">
                    <Dropdown name="expitationYear" items={this.expirationYears} placeholder="Year" value={null}/>
                  </div>
                </div>
              </li>
              <li className="fc-credit-card-form-line">
                <div>
                  <label>
                    Billing Address
                  </label>
                  <input type="hidden" name="billingAddressId" id="billingAddressIdCardFormField" />
                  <div className="fc-credit-card-form-address-book">
                  </div>
                </div>
              </li>
            </ul>
          </div>
          <div className="fc-credit-card-form-controls">
            <a className="fc-btn-link" onClick={ this.props.onCancel }>Cancel</a>
            <PrimaryButton type="submit">Save</PrimaryButton>
          </div>
        </Form>
      </li>
    );
  }
}
