import _ from 'lodash';
import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import { Checkbox } from '../checkbox/checkbox';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import AddressDetails from '../addresses/address-details';
import AddressSelect from '../addresses/address-select';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as CustomersActions from '../../modules/customers/details';
import * as CardUtils from '../../lib/credit-card-utils';

@connect((state, props) => ({
  ...state.customers.details[props.customerId]
}), CustomersActions)
export default class EditCreditCardBox extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      editingAddress: false
    };
  }

  componentDidMount() {
    const customer = this.props.customerId;
    this.props.fetchAdresses(customer);
  }

  get addressBlock() {
    console.log(this.props);
    const block = this.state.editingAddress ?
                    ( <AddressSelect name="addressId"
                                     items={ this.props.addresses }
                                     value={ this.props.form.addressId }
                                     onItemSelect={ this.onAddressChange } />) :
                    ( <AddressDetails customerId={ this.props.customerId }
                                      address={ this.props.card } />);
    return block;
  }

  @autobind
  toggleSelectAddress() {
    const newState = { editingAddress: !this.state.editingAddress };
    this.setState(newState);
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
    const card = this.props.card;
    const form = this.props.form;
    return (
      <li className="fc-card-container fc-credit-cards fc-credit-cards-edit">
        <Form className="fc-customer-credit-card-form fc-form-vertical"
              onChange={ this.props.onChange }
              onSubmit={ this.props.onSubmit }>
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
                         value={ form.holderName }
                         required />
                </FormField>
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
                    Billing Address - <a className="fc-btn-link" onClick={ this.toggleSelectAddress }>Change</a>
                  </label>
                  { this.addressBlock }
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
