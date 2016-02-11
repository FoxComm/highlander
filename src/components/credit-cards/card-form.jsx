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
    isDefaultEnabled: PropTypes.bool,
    form: PropTypes.object,
    addresses: PropTypes.array,
    card: PropTypes.shape({
      address: PropTypes.shape({
        id: PropTypes.number
      })
    }),
    customerId: PropTypes.number,
    className: PropTypes.string,
    showFormControls: PropTypes.bool,
  };

  static defaultProps = {
    isDefaultEnabled: true,
    showFormControls: true,
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
      <li className="fc-credit-card-form__line">
        <div className="fc-grid">
          <div className="fc-col-md-3-4">
            <FormField label="Card Number"
                       labelClassName="fc-credit-card-form__label"
                       validator="ascii">
              <input id="numberCardFormField"
                     className="fc-credit-card-form__input"
                     name="number"
                     maxLength="255"
                     type="text"
                     required
                     value={ form.number }/>
            </FormField>
          </div>
          <div className="fc-col-md-1-4">
            <FormField label="CVV"
                       labelClassName="fc-credit-card-form__label"
                       validator="ascii">
              <input id="cvvCardFormField"
                     className="fc-credit-card-form__input"
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
        <li className="fc-credit-card-form__addresses">
          <div>
            <label className="fc-credit-card-form__label">
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
        <li className="fc-credit-card-form__line">
          <div>
            <label className="fc-credit-card-form__label">
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
        <header className="fc-credit-card-form__header">
          New Credit Card
        </header>
      );
    }
  }

  get defaultCheckbox() {
    const { form, isDefaultEnabled } = this.props;

    const className = classNames('fc-credit-card-form__default', {
      '_disabled': !isDefaultEnabled,
    });

    return (
      <li className="fc-credit-card-form__line">
        <label className={className}>
          <Checkbox disabled={!isDefaultEnabled}
                    defaultChecked={form.isDefault}
                    className="fc-credit-card-form__default-checkbox"
                    name="isDefault" />
          <span className="fc-credit-card-form__default-label">Default Card</span>
        </label>
      </li>
    );
  }

  render() {
    const {form, isDefaultEnabled, onChange, onSubmit, onCancel} = this.props;
    const className = classNames('fc-credit-card-form fc-form-vertical', this.props.className);


    return (
      <Form className={ className }
            onChange={ onChange }
            onSubmit={ onSubmit }>
        { this.header }
        <div>
          <ul className="fc-credit-card-form__fields">
            {this.defaultCheckbox}
            <li className="fc-credit-card-form__line">
              <FormField label="Name on Card"
                         validator="ascii"
                         labelClassName="fc-credit-card-form__label">
                <input id="nameCardFormField"
                       className="fc-credit-card-form__input"
                       name="holderName"
                       maxLength="255"
                       type="text"
                       required
                       value={ form.holderName } />
              </FormField>
            </li>
            { this.cardNumberBlock }
            <li className="fc-credit-card-form__line">
              <label className="fc-credit-card-form__label">Expiration Date</label>
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
        {this.props.showFormControls && <SaveCancel onCancel={onCancel} />}
      </Form>
    );
  }

}
