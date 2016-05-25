// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// utils
import * as CardUtils from '../../lib/credit-card-utils';
import { detectCardType, cardMask, cvvLength, isCardNumberValid, isCvvValid } from 'wings/lib/payment-cards';

// components
import { Checkbox } from '../checkbox/checkbox';
import FormField from '../forms/formfield';
import Form from '../forms/form';
import Dropdown from '../dropdown/dropdown';
import AddressDetails from '../addresses/address-details';
import AddressSelect from '../addresses/address-select';
import SaveCancel from '../common/save-cancel';
import InputMask from 'react-input-mask';

import * as AddressActions from '../../modules/customers/addresses';

function mapStateToProps(state, props) {
  return {
    addresses: _.get(state.customers.addresses, [props.customerId, 'addresses'], []),
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: {
      ...bindActionCreators(AddressActions, dispatch),
    },
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class CreditCardForm extends React.Component {

  static propTypes = {
    onChange: PropTypes.func,
    onSubmit: PropTypes.func,
    onCancel: PropTypes.func,
    isNew: PropTypes.bool,
    isDefaultEnabled: PropTypes.bool,
    addresses: PropTypes.array,
    card: PropTypes.shape({
      address: PropTypes.shape({
        id: PropTypes.number
      })
    }),
    customerId: PropTypes.number,
    className: PropTypes.string,
    saveText: PropTypes.string,
  };

  static defaultProps = {
    isDefaultEnabled: true,
    onChange: _.noop,
    saveText: 'Save',
  };

  state = {
    card: this.props.card,
    editingAddress: this.props.isNew,
  };

  componentDidMount() {
    this.props.actions.fetchAddresses(this.props.customerId);
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
    const { isDefaultEnabled } = this.props;

    const className = classNames('fc-credit-card-form__default', {
      '_disabled': !isDefaultEnabled,
    });

    const isDefault = _.get(this.state, 'card.isDefault', false);

    return (
      <li className="fc-credit-card-form__line">
        <label className={className}>
          <Checkbox disabled={!isDefaultEnabled}
                    defaultChecked={isDefault}
                    className="fc-credit-card-form__default-checkbox"
                    name="isDefault"
                    id="isDefault" />
          <span className="fc-credit-card-form__default-label">
            Default Card
          </span>
        </label>
      </li>
    );
  }

  get nameBlock() {
    const holderName = _.get(this.state, 'card.holderName', '');
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
               value={holderName} />
        </FormField>
      </li>
    );
  }

  get cardNumber() {
    return _.get(this.state, 'card.cardNumber', '');
  }

  get cardType() {
    return detectCardType(this.cardNumber);
  }

  get cardMask() {
    return cardMask(this.cardType);
  }

  @autobind
  validateCardNumber() {
    return isCardNumberValid(this.cardNumber) ? null : 'Please enter a valid credit card number';
  }

  @autobind
  validateCvvNumber() {
    const cvv = _.get(this.state, 'card.cvv', '');

    return isCvvValid(cvv, this.cardType) ? null : `Please enter a valid cvv number`;
  }

  @autobind
  changeCardNumber({target}) {
    const value = target.value.replace(/[^\d]/g, '');

    const newState = assoc(this.state, ['card', 'cardNumber'], value);
    const data = { target: { name: 'cardNumber', value } };
    this.setState(newState, () => this.props.onChange(data));
  }

  get cardNumberBlock() {
    const { isNew } = this.props;
    const cardNumber = _.get(this.state, 'card.cardNumber', '');
    const cvv = _.get(this.state, 'card.cvv', '');

    if (!isNew) {
      return null;
    }

    return (
      <li className="fc-credit-card-form__line">
        <div className="fc-grid">
          <div className="fc-col-md-3-4">
            <FormField label="Card Number"
                       labelClassName="fc-credit-card-form__label"
                       validator={this.validateCardNumber}>
              <InputMask
                id="numberCardFormField"
                className="fc-credit-card-form__input"
                name="cardNumber"
                maskChar=" "
                size="20"
                mask={this.cardMask}
                type="text"
                required
                value={cardNumber}
                onChange={this.changeCardNumber}
              />
            </FormField>
          </div>
          <div className="fc-col-md-1-4">
            <FormField label="CVV"
                       labelClassName="fc-credit-card-form__label"
                       validator={this.validateCvvNumber}>
              <input id="cvvCardFormField"
                     className="fc-credit-card-form__input"
                     name="cvv"
                     maxLength={cvvLength(this.cardType)}
                     type="text"
                     required
                     value={cvv}/>
            </FormField>
          </div>
        </div>
      </li>
    );
  }

  get expirationBlock() {
    const expMonth = _.get(this.state, 'card.expMonth');
    const expYear = _.get(this.state, 'card.expYear');

    return (
      <li className="fc-credit-card-form__line">
        <label className="fc-credit-card-form__label">Expiration Date</label>
        <div className="fc-grid">
          <div className="fc-col-md-1-2">
            <Dropdown name="expMonth"
                      items={CardUtils.monthList()}
                      placeholder="Month"
                      value={expMonth}
                      onChange={this.onExpMonthChange} />
          </div>
          <div className="fc-col-md-1-2">
            <Dropdown name="expYear"
                      items={CardUtils.expirationYears()}
                      placeholder="Year"
                      value={expYear}
                      onChange={this.onExpYearChange} />
          </div>
        </div>
      </li>
    );
  }

  get billingAddress() {
    const { customerId } = this.props;
    const address = _.get(this.state, 'card.address');

    let addressDetails = null;
    let changeLink = null;

    if (!this.state.editingAddress) {
      changeLink = (
        <span>
          - <a className="fc-btn-link" onClick={this.toggleSelectAddress}>Change</a>
        </span>
      );

      addressDetails = <AddressDetails customerId={customerId} address={address} />;
    }

    return (
      <li className="fc-credit-card-form__line">
        <div>
          <label className="fc-credit-card-form__label">
            Billing Address {changeLink}
          </label>
          {addressDetails}
        </div>
      </li>
    );
  }

  get addressBook() {
    const { addresses, customerId } = this.props;
    const addressId = _.get(this.state, 'card.address.id');

    if (this.state.editingAddress) {
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
    return (
      <SaveCancel saveText={this.props.saveText}
                  onCancel={this.props.onCancel} />
    );
  }

  @autobind
  onChange({target}) {
    const newState = assoc(this.state, ['card', target.name], target.value);
    this.setState(newState, () => this.props.onChange({target}));
  }

  @autobind
  onExpYearChange(value) {
    const payload = { target: { name: 'expYear', value: +value } };
    this.setState(
      assoc(this.state, ['card', 'expYear'], +value),
      () => this.props.onChange(payload)
    );
  }

  @autobind
  onExpMonthChange(value) {
    const payload = { target: { name: 'expMonth', value: +value } };
    this.setState(
      assoc(this.state, ['card', 'expMonth'], +value),
      () => this.props.onChange(payload)
    );
  }

  @autobind
  onAddressChange(value) {
    const addressId = +value;
    const address = _.find(this.props.addresses, { id: addressId });
    const payload = { target: { name: 'addressId', value: addressId } };

    const newState = assoc(this.state,
      'editingAddress', false,
      ['card', 'addressId'], addressId,
      ['card', 'address'], address
    );

    this.setState(newState, () => this.props.onChange(payload));
  }

  @autobind
  toggleSelectAddress() {
    this.setState({ editingAddress: !this.state.editingAddress });
  }

  render() {
    const { className, onSubmit } = this.props;
    const formClassName = classNames('fc-credit-card-form fc-form-vertical', className);

    return (
      <Form className={formClassName}
            onChange={this.onChange}
            onSubmit={(event) => onSubmit(event, this.state.card)}>
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
