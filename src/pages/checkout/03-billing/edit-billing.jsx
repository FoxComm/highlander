/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import textStyles from 'ui/css/input.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { cardMask } from 'wings/lib/payment-cards';
import localized from 'lib/i18n';
import { api as foxApi } from 'lib/api';

// components
import { FormField } from 'ui/forms';
import { TextInput, TextInputWithLabel } from 'ui/inputs';
import Checkbox from 'ui/checkbox/checkbox';
import Autocomplete from 'ui/autocomplete';
import InputMask from 'react-input-mask';
import EditAddress from 'ui/address/edit-address';
import CreditCards from './credit-cards';
import Icon from 'ui/icon';
import CvcHelp from './cvc-help';
import PromoCode from '../../../components/promo-code/promo-code';
import CheckoutForm from '../checkout-form';
import Accordion from '../../../components/accordion/accordion';

// styles
import styles from './billing.css';

// actions
import * as cartActions from 'modules/cart';
import * as checkoutActions from 'modules/checkout';

// types
import type { CreditCardType, CheckoutActions } from '../types';

type Props = CheckoutActions & {
  error: Array<any>,
  data: CreditCardType,
  billingData: ?CreditCardType,
  continueAction: Function,
  performStageTransition: Function,
  t: any,
  inProgress: boolean,
  saveCouponCode: Function,
  removeCouponCode: Function,
  coupon: ?Object,
  promotion: ?Object,
  totals: Object,
};

type State = {
  addingNew: boolean,
  billingAddressIsSame: boolean,
  cardAdded: boolean,
};

function numbersComparator(value1, value2) {
  return Number(value1) === Number(value2);
}

class EditBilling extends Component {
  props: Props;

  state: State = {
    addingNew: false,
    billingAddressIsSame: true,
    cardAdded: false,
  };

  componentWillMount() {
    if (this.props.data.address) {
      this.setState({
        billingAddressIsSame: false,
      });
    }
  }

  @autobind
  handleSubmit() {
    this.props.continueAction();
  }

  @autobind
  changeFormData({ target }) {
    this.props.setBillingData(target.name, target.value);
  }

  @autobind
  changeCVC({ target }) {
    const value = target.value.replace(/[^\d]/g, '').substr(0, target.maxLength);
    this.props.setBillingData('cvc', value);
  }

  @autobind
  changeCardNumber({ target }) {
    const value = target.value.replace(/[^\d]/g, '');

    this.props.setBillingData('number', value);
    this.props.setBillingData('brand', foxApi.creditCards.cardType(value));
    this.props.setBillingData('lastFour', value.substr(-4));
  }

  @autobind
  changeMonth(month) {
    this.props.setBillingData('expMonth', month);
  }

  @autobind
  changeYear(year) {
    this.props.setBillingData('expYear', year);
  }

  @autobind
  changeDefault(value) {
    this.props.setBillingData('isDefault', value);
  }

  get billingAddress() {
    const { billingAddressIsSame } = this.state;

    if (billingAddressIsSame) {
      return null;
    }

    return (
      <EditAddress
        {...this.props}
        address={this.props.data.address}
        onUpdate={this.props.setBillingAddress}
      />
    );
  }

  // Possible values: https://stripe.com/docs/stripe.js?#card-cardType
  get cardType() {
    const { number } = this.props.data;
    return foxApi.creditCards.cardType(number);
  }

  get cardMask() {
    return cardMask(this.cardType);
  }

  get paymentIcon() {
    if (this.cardType) {
      return (
        <Icon
          styleName="payment-icon"
          name={`fc-payment-${_.kebabCase(this.cardType)}`}
        />
      );
    }
  }

  @autobind
  validateCardNumber() {
    const { number } = this.props.data;
    const { t } = this.props;

    return foxApi.creditCards.validateCardNumber(number) ? null : t('Please enter a valid credit card number');
  }

  @autobind
  validateCvcNumber() {
    const { cvc } = this.props.data;
    const { t } = this.props;

    return foxApi.creditCards.validateCVC(cvc) ? null : t(`Please enter a valid cvc number`);
  }

  @autobind
  addNew() {
    this.props.resetBillingData();
    this.setState({ addingNew: true });
  }

  @autobind
  cancelEditing() {
    this.props.performStageTransition('billingInProgress', () => {
      this.setState({ addingNew: false, cardAdded: false });
    });
  }

  @autobind
  selectCreditCard(creditCard) {
    this.props.selectCreditCard(creditCard);
    this.setState({ addingNew: false });
  }

  @autobind
  updateCreditCard() {
    const id = _.get(this.props, 'billingData.id');
    const { billingAddressIsSame } = this.state;

    this.props.performStageTransition('isProceedingCard', () => {
      const operation = id
        ? this.props.updateCreditCard(id, billingAddressIsSame)
        : this.props.addCreditCard(billingAddressIsSame);
      return operation.then(() => this.setState({ addingNew: false, cardAdded: (id === undefined) }));
    });
  }

  @autobind
  deleteCreditCard(id) {
    this.props.deleteCreditCard(id);
  }

  @autobind
  editCard(data) {
    this.props.loadBillingData(data);
    this.setState({ addingNew: true });
  }

  @autobind
  toggleSeparateBillingAddress() {
    this.setState({
      billingAddressIsSame: !this.state.billingAddressIsSame,
    });
  }

  get editCardForm() {
    const { props } = this;
    const { data, t } = props;

    const months = _.range(1, 13, 1).map(x => _.padStart(x.toString(), 2, '0'));
    const currentYear = new Date().getFullYear();
    const years = _.range(currentYear, currentYear + 10, 1).map(x => x.toString());
    const checkedDefaultCard = _.get(data, 'isDefault', false);
    const editingSavedCard = data.id;
    const cardNumberPlaceholder = editingSavedCard ?
      (_.repeat('**** ', 3) + data.lastFour) : t('CARD NUMBER');
    const cvcPlaceholder = editingSavedCard ? '***' : 'CVC';

    return (
      <div styleName="edit-card-form">
        <Checkbox
          styleName="checkbox-field"
          name="isDefault"
          checked={checkedDefaultCard}
          onChange={({target}) => this.changeDefault(target.checked)}
          id="set-default-card"
        >
          Make this card my default
        </Checkbox>
          <FormField styleName="text-field">
            <TextInput
              required
              name="holderName"
              placeholder={t('NAME ON CARD')}
              value={data.holderName}
              onChange={this.changeFormData}
            />
          </FormField>
          <div styleName="union-fields">
            <FormField styleName="card-number-field" validator={this.validateCardNumber}>
              <TextInputWithLabel
                label={this.paymentIcon}
              >
                <InputMask
                  required
                  disabled={editingSavedCard}
                  styleName="payment-input"
                  className={textStyles['text-input']}
                  maskChar=" "
                  type="text"
                  mask={this.cardMask}
                  name="number"
                  placeholder={cardNumberPlaceholder}
                  size="20"
                  value={data.number}
                  onChange={this.changeCardNumber}
                />
              </TextInputWithLabel>
            </FormField>
            <FormField styleName="cvc-field" validator={this.validateCvcNumber}>
              <TextInputWithLabel
                required
                disabled={editingSavedCard}
                label={<CvcHelp />}
                type="number"
                maxLength="4"
                placeholder={cvcPlaceholder}
                onChange={this.changeCVC}
                value={data.cvc}
              />
            </FormField>
          </div>
          <div styleName="union-fields">
            <FormField required styleName="text-field" getTargetValue={() => data.expMonth}>
              <Autocomplete
                inputProps={{
                  placeholder: t('MONTH'),
                  type: 'text',
                }}
                compareValues={numbersComparator}
                getItemValue={item => item}
                items={months}
                onSelect={this.changeMonth}
                selectedItem={data.expMonth}
              />
            </FormField>
            <FormField required styleName="text-field" getTargetValue={() => data.expYear}>
              <Autocomplete
                inputProps={{
                  placeholder: t('YEAR'),
                  type: 'text',
                }}
                compareValues={numbersComparator}
                allowCustomValues
                getItemValue={item => item}
                items={years}
                onSelect={this.changeYear}
                selectedItem={data.expYear}
              />
            </FormField>
          </div>
          <Checkbox
            id="billingAddressIsSame"
            checked={this.state.billingAddressIsSame}
            onChange={this.toggleSeparateBillingAddress}
            styleName="same-address-checkbox"
          >
            {t('Billing address is same as shipping')}
          </Checkbox>
          {this.billingAddress}
      </div>
    );
  }

  renderGiftCard() {
    const { giftCards } = this.props;
    const giftCard = _.find(giftCards, { type: 'giftCard' });

    return (
      <PromoCode
        buttonLabel="Redeem"
        giftCard={giftCard}
        saveCode={this.props.saveGiftCard}
        removeCode={this.props.removeGiftCard}
      />
    );
  }

  render() {
    const { inProgress, t } = this.props;

    if (this.state.addingNew) {
      const action = {
        action: this.cancelEditing,
        title: 'Cancel',
      };

      return (
        <CheckoutForm
          submit={this.updateCreditCard}
          title={t('Add Card')}
          error={this.props.error}
          buttonLabel="SAVE & CONTINUE"
          action={action}
          inProgress={inProgress}
        >
          {this.editCardForm}
        </CheckoutForm>
      );
    }

    return (
      <CheckoutForm
        submit={this.handleSubmit}
        title="PAYMENT METHOD"
        error={this.props.error}
        buttonLabel="Place Order"
        inProgress={inProgress}
      >
        <fieldset styleName="fieldset-cards">
          <CreditCards
            selectCreditCard={this.selectCreditCard}
            editCard={this.editCard}
            deleteCard={this.deleteCreditCard}
            cardAdded={this.state.cardAdded}
          />
          <button onClick={this.addNew} type="button" styleName="add-card-button">Add Card</button>
        </fieldset>

        <Accordion title="PROMO CODE?">
          <PromoCode
            coupon={this.props.coupon}
            promotion={this.props.promotion}
            discountValue={this.props.totals.adjustments}
            saveCode={this.props.saveCouponCode}
            removeCode={this.props.removeCouponCode}
          />
        </Accordion>

        <Accordion title="GIFT CARD?">
          { this.renderGiftCard() }
        </Accordion>

      </CheckoutForm>
    );
  }
}

function mapStateToProps(state) {
  return {
    data: state.checkout.billingData,
    ...state.cart,
  };
}

export default connect(mapStateToProps, { ...checkoutActions, ...cartActions })(localized(EditBilling));
