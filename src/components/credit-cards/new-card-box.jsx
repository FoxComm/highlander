import _ from 'lodash';
import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import { Checkbox } from '../checkbox/checkbox';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import AddressSelect from '../addresses/address-select';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as CustomersActions from '../../modules/customers/details';
import * as CardUtils from '../../lib/credit-card-utils';

@connect((state, props) => ({
  ...state.customers.details[props.customerId]
}), CustomersActions)
export default class NewCreditCardBox extends React.Component {

  componentDidMount() {
    const customer = this.props.customerId;
    this.props.fetchAdresses(customer);
  }

  @autobind
  onExpYearChange(value) {
    this.props.onChange( {target: {name: 'expYear', value: +value} } );
  }

  @autobind
  onExpMonthChange(value) {
    this.props.onChange( {target: {name: 'expMonth', value: +value} } );
  }

  @autobind
  onAddressChange(value) {
    this.props.onChange( {target: {name: 'addressId', value: +value} } );
  }

  render() {
    const form = this.props.form;
    return (
      <li className="fc-card-container fc-credit-cards fc-credit-cards-new">
        <Form className="fc-customer-credit-card-form fc-form-vertical"
              onChange={ this.props.onChange }
              onSubmit={ this.props.onSubmit }>
          <header>
            New Credit Card
          </header>
          <div>
            <ul className="fc-credit-card-form-fields">
              <li className="fc-credit-card-form-line">
                <label className="fc-credit-card-default-checkbox">
                  <Checkbox defaultChecked={ form.isDefault } name="isDefault" />
                  <span>Default Card</span>
                </label>
              </li>
              <li className="fc-credit-card-form-line">
                <FormField label="Name on Card" validator="ascii">
                  <input id="nameCardFormField"
                         className="fc-customer-form-input"
                         name="holderName"
                         maxLength="255"
                         type="text"
                         required
                         value={ form.holderName } />
                </FormField>
              </li>
              <li className="fc-credit-card-form-line">
                <div className="fc-grid">
                  <div className="fc-col-md-3-4">
                    <FormField label="Card Number" validator="ascii">
                      <input id="numberCardFormField"
                             className="fc-customer-form-input"
                             name="number"
                             maxLength="255"
                             type="text"
                             required
                             value={ form.number } />
                    </FormField>
                  </div>
                  <div className="fc-col-md-1-4">
                    <FormField label="CVV" validator="ascii">
                      <input id="cvvCardFormField"
                             className="fc-customer-form-input"
                             name="cvv"
                             maxLength="255"
                             type="text"
                             required
                             value={ form.cvv } />
                    </FormField>
                  </div>
                </div>
              </li>
              <li className="fc-credit-card-form-line">
                <label>Expiration Date</label>
                <div className="fc-grid">
                  <div className="fc-col-md-1-2">
                    <Dropdown name="expMonth"
                              items={ CardUtils.monthList() }
                              placeholder="Month"
                              value={ form.expMonth }
                              onChange={ this.onExpMonthChange } />
                  </div>
                  <div className="fc-col-md-1-2">
                    <Dropdown name="expYear"
                              items={ CardUtils.expirationYears() }
                              placeholder="Year"
                              value={ form.expYear }
                              onChange={ this.onExpYearChange } />
                  </div>
                </div>
              </li>
              <li className="fc-credit-card-form-line">
                <div>
                  <label>
                    Billing Address
                  </label>
                  <AddressSelect name="addressId"
                                 items={ this.props.addresses }
                                 value={ form.addressId }
                                 onItemSelect={ this.onAddressChange } />
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
