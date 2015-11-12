import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import { Checkbox } from '../checkbox/checkbox';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import AddressDetails from '../addresses/address-details';
import AddressSelect from '../addresses/address-select';
import { autobind } from 'core-decorators';
import * as CardUtils from '../../lib/credit-card-utils';
import classnames from 'classnames';

export default class CreditCardForm extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      editingAddress: false
    };
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

  @autobind
  toggleSelectAddress() {
    const newState = { editingAddress: !this.state.editingAddress };
    this.setState(newState);
  }

  get cardNumberBlock() {
    let block = null;
    if (this.props.isNew) {
      block = (
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
                       value={ this.props.form.number } />
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
                       value={ this.props.form.cvv } />
              </FormField>
            </div>
          </div>
        </li>
      );
    }
    return block;
  }


  get addressEditBlock() {
    const block = this.state.editingAddress ?
                    ( <AddressSelect name="addressId"
                                     items={ this.props.addresses }
                                     value={ this.props.form.addressId }
                                     customerId={ this.props.customerId }
                                     onItemSelect={ this.onAddressChange } />) :
                    ( <AddressDetails customerId={ this.props.customerId }
                                      address={ this.props.card } />);
    return block;
  }

  get addressSelectBlock() {
    let block = null;
    if (this.props.isNew) {
      block = (
        <li className="fc-credit-card-form-line">
          <div>
            <label>
              Billing Address
            </label>
            <AddressSelect name="addressId"
                           customerId={ this.props.customerId }
                           items={ this.props.addresses }
                           value={ this.props.form.addressId }
                           onItemSelect={ this.onAddressChange } />
          </div>
        </li>
      );
    } else {
      block = (
        <li className="fc-credit-card-form-line">
          <div>
            <label>
              Billing Address - <a className="fc-btn-link" onClick={ this.toggleSelectAddress }>Change</a>
            </label>
            { this.addressEditBlock }
          </div>
        </li>
      );
    }
    return block;
  }

  render() {
    const form = this.props.form;
    const containerClass = classnames(
      'fc-card-container',
      'fc-credit-cards',
      {'fc-credit-cards-new' : this.props.isNew },
      {'fc-credit-cards-edit' : !this.props.isNew }
    );
    return (
      <li className={ containerClass }>
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
              { this.cardNumberBlock }
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
              { this.addressSelectBlock }
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
