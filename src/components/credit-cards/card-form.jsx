// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// utils
import * as CardUtils from '../../lib/credit-card-utils';

// components
import { Checkbox } from '../checkbox/checkbox';
import FormField from '../forms/formfield';
import Form from '../forms/form';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import AddressDetails from '../addresses/address-details';
import AddressSelect from '../addresses/address-select';
import SaveCancel from '../common/save-cancel';

export default class CreditCardForm extends React.Component {

  static propTypes = {
    onChange: PropTypes.func,
    onSubmit: PropTypes.func,
    onCancel: PropTypes.func,
    isNew: PropTypes.bool,
    form: PropTypes.object,
    addresses: PropTypes.array,
    card: PropTypes.shape({
      address: PropTypes.shape({
        id: PropTypes.number
      })
    }),
    customerId: PropTypes.number
  };

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
    const {isNew, form} = this.props;

    if (!isNew) {
      return null;
    }

    return (
      <li className="fc-credit-card-form-line">
        <div className="fc-grid">
          <div className="fc-col-md-3-4">
            <FormField label="Card Number"
                       labelClassName="fc-credit-card-form-label"
                       validator="ascii">
              <input id="numberCardFormField"
                     className="fc-credit-card-form-input"
                     name="number"
                     maxLength="255"
                     type="text"
                     required
                     value={ form.number }/>
            </FormField>
          </div>
          <div className="fc-col-md-1-4">
            <FormField label="CVV"
                       labelClassName="fc-credit-card-form-label"
                       validator="ascii">
              <input id="cvvCardFormField"
                     className="fc-credit-card-form-input"
                     name="cvv"
                     maxLength="255"
                     type="text"
                     required
                     value={ form.cvv }/>
            </FormField>
          </div>
        </div>
      </li>
    );
  }

  get addressEditBlock() {
    const {addresses, card, customerId} = this.props;

    return this.state.editingAddress
      ? ( <AddressSelect name="addressId"
                         items={ addresses }
                         initialValue={ card.address.id }
                         customerId={ customerId }
                         onItemSelect={ this.onAddressChange }/>)
      : ( <AddressDetails customerId={ customerId }
                          address={ card.address }/>);
  }

  get addressSelectBlock() {
    const {isNew, addresses, customerId} = this.props;
    let block = null;
    let addressId = _.get(this.props, 'card.address.id');

    if (isNew) {
      block = (
        <li className="fc-credit-card-form-line">
          <div>
            <label className="fc-credit-card-form-label">
              Billing Address
            </label>
            <AddressSelect name="addressId"
                           customerId={ customerId }
                           items={ addresses }
                           initialValue={ addressId }
                           onItemSelect={ this.onAddressChange } />
          </div>
        </li>
      );
    } else {
      block = (
        <li className="fc-credit-card-form-line">
          <div>
            <label className="fc-credit-card-form-label">
              Billing Address - <a className="fc-btn-link" onClick={ this.toggleSelectAddress }>Change</a>
            </label>
            { this.addressEditBlock }
          </div>
        </li>
      );
    }

    return block;
  }

  get header() {
    if (this.props.isNew) {
      return (
        <header className="fc-credit-card-form-header">
          New Credit Card
        </header>
      );
    }
  }

  render() {
    const {form, isNew, onChange, onSubmit, onCancel} = this.props;
    const containerClass = classNames(
      'fc-card-container',
      'fc-credit-cards',
      {'fc-credit-cards-new' : isNew },
      {'fc-credit-cards-edit' : !isNew }
    );

    return (
      <li className={ containerClass }>
        <Form className="fc-customer-credit-card-form fc-form-vertical"
              onChange={ onChange }
              onSubmit={ onSubmit }>
          { this.header }
          <div>
            <ul className="fc-credit-card-form-fields">
              <li className="fc-credit-card-form-line">
                <label className="fc-credit-card-default-checkbox">
                  <Checkbox defaultChecked={ form.isDefault } name="isDefault" />
                  <span>Default Card</span>
                </label>
              </li>
              <li className="fc-credit-card-form-line">
                <FormField label="Name on Card"
                           validator="ascii"
                           labelClassName="fc-credit-card-form-label">
                  <input id="nameCardFormField"
                         className="fc-credit-card-form-input"
                         name="holderName"
                         maxLength="255"
                         type="text"
                         required
                         value={ form.holderName } />
                </FormField>
              </li>
              { this.cardNumberBlock }
              <li className="fc-credit-card-form-line">
                <label className="fc-credit-card-form-label">Expiration Date</label>
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
          <SaveCancel className="fc-credit-card-form-controls"
                      onCancel={onCancel}
                      cancelClassName="fc-credit-card-form-link"
                      saveClassName="fc-credit-card-form-button"/>
        </Form>
      </li>
    );
  }

}
