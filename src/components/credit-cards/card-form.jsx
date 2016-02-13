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
    showSelectedAddress: PropTypes.bool,
  };

  static defaultProps = {
    isDefaultEnabled: true,
    showFormControls: true,
    showSelectedAddress: false,
  };

  constructor(...args) {
    super(...args);
    this.state = {
      editingAddress: this.props.isNew,
    };
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

  get defaultCheckboxBlock() {
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
          <span className="fc-credit-card-form__default-label">
            Default Card
          </span>
        </label>
      </li>
    );
  }

  get nameBlock() {
    return (
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
               value={this.props.form.holderName} />
        </FormField>
      </li>
    );
  }

  get cardNumberBlock() {
    const { isNew, form } = this.props;

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
                     value={form.number}/>
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
                     value={form.cvv}/>
            </FormField>
          </div>
        </div>
      </li>
    );
  }

  get expirationBlock() {
    const { form } = this.props;

    return (
      <li className="fc-credit-card-form__line">
        <label className="fc-credit-card-form__label">Expiration Date</label>
        <div className="fc-grid">
          <div className="fc-col-md-1-2">
            <Dropdown name="expMonth"
                      items={CardUtils.monthList()}
                      placeholder="Month"
                      value={form.expMonth}
                      onChange={this.onExpMonthChange} />
          </div>
          <div className="fc-col-md-1-2">
            <Dropdown name="expYear"
                      items={CardUtils.expirationYears()}
                      placeholder="Year"
                      value={form.expYear}
                      onChange={this.onExpYearChange} />
          </div>
        </div>
      </li>
    );
  }

  get billingAddress() {
    const { card, customerId, isNew, showSelectedAddress } = this.props;
    const addressId = _.get(card, 'address.id');

    let address = null;
    let changeLink = null;

    if (!this.state.editingAddress && showSelectedAddress) {
      changeLink = (
        <span>
          - <a className="fc-btn-link" onClick={this.toggleSelectAddress}>Change</a>
        </span>
      );

      address = <AddressDetails customerId={customerId} address={card.address} />;
    }

    return (
      <li className="fc-credit-card-form__line">
        <div>
          <label className="fc-credit-card-form__label">
            Billing Address {changeLink}
          </label>
          {address}
        </div>
      </li>
    );
  }

  get addressBook() {
    const { isNew, addresses, customerId, showSelectedAddress } = this.props;
    const addressId = _.get(this.props, 'card.address.id');

    if (this.state.editingAddress || !showSelectedAddress) {
      return (
        <li className="fc-credit-card-form__addresses">
          <div>
            <AddressSelect name="addressId"
                           customerId={customerId}
                           items={addresses}
                           initialValue={addressId}
                           onItemSelect={this.onAddressChange} />
          </div>
        </li>
      );
    }
  }

  get submit() {
    if (this.props.showFormControls) {
      return <SaveCancel onCancel={this.props.onCancel} />;
    }
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
    const changeValue = {
      target: {
        name: 'addressId',
        value: +value,
      },
    };

    this.setState({
      editingAddress: false,
    }, () => this.props.onChange(changeValue));
  }

  @autobind
  toggleSelectAddress() {
    const newState = { editingAddress: !this.state.editingAddress };
    this.setState(newState);
  }


  render() {
    const {form, isDefaultEnabled, onChange, onSubmit, onCancel} = this.props;
    const className = classNames('fc-credit-card-form fc-form-vertical', this.props.className);

    return (
      <Form className={className}
            onChange={onChange}
            onSubmit={onSubmit}>
        {this.header}
        <div>
          <ul className="fc-credit-card-form__fields">
            {this.defaultCheckboxBlock}
            {this.nameBlock}
            {this.cardNumberBlock}
            {this.expirationBlock}
            {this.billingAddress}
            {this.addressBook}
          </ul>
        </div>
        {this.submit}
      </Form>
    );
  }
}
