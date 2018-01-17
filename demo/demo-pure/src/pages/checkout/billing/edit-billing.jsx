/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import textStyles from 'ui/text-input/text-input.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { cardMask } from '@foxcommerce/wings/lib/payment-cards';
import localized from 'lib/i18n';
import { api as foxApi } from 'lib/api';
import { createNumberMask } from 'lib/i18n/field-masks';

// components
import { FormField } from 'ui/forms';
import { TextInput } from 'ui/text-input';
import Checkbox from 'ui/checkbox/checkbox';
import Autocomplete from 'ui/autocomplete';
import MaskedInput from 'react-text-mask';
import EditAddress from 'ui/address/edit-address';
import CreditCards from './credit-cards';
import Icon from 'ui/icon';
import PromoCode from 'components/promo-code/promo-code';
import CheckoutForm from '../checkout-form';
import ActionLink from 'ui/action-link/action-link';
import { AddressDetails } from 'ui/address';
import EditPromos from 'components/promo-code/edit-promos';

// styles
import styles from './billing.css';

// actions
import * as cartActions from 'modules/cart';
import * as checkoutActions from 'modules/checkout';

// types
import type { CreditCardType, CheckoutActions } from '../types';
import type { AsyncStatus } from 'types/async-actions';

type Props = CheckoutActions & {
  error: Array<any>,
  data: CreditCardType,
  billingData: ?CreditCardType,
  t: any,
  saveCouponCode: () => Promise<*>,
  removeCouponCode: () => Promise<*>,
  coupon: ?Object,
  updateCreditCardInProgress: boolean,
  updateCreditCardError: void|Object,
  checkoutState: AsyncStatus,
  giftCards: Array<Object>,
  isGuestMode: boolean,
  clearAddCreditCardErrors: () => void,
  clearUpdateCreditCardErrors: () => void,
  creditCards: Array<CreditCardType>,
  creditCardsLoading: boolean,
  chooseCreditCard: () => Promise<*>,
};

type State = {
  addingNew: boolean,
  billingAddressIsSame: boolean,
  cardAdded: boolean,
  selectedCard: CreditCardType,
  addingGC: boolean,
  addingCoupon: boolean,
  couponCode: string,
  gcCode: string,
  error: any,
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
    selectedCard: {},
    addingGC: false,
    addingCoupon: false,
    couponCode: '',
    gcCode: '',
    error: false,
  };

  componentWillMount() {
    const chosenCreditCard = _.find(this.props.paymentMethods, {type: 'creditCard'});
    if (chosenCreditCard) {
      this.props.selectCreditCard(chosenCreditCard);
    }

    const coupon = this.props.coupon;
    if (!_.isEmpty(coupon)) {
      this.setState({ couponCode: coupon.code });
    }

    if (this.props.data.address) {
      this.state.billingAddressIsSame = false;
    }
  }

  // Credit Card Handling
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

  @autobind
  cardMask() {
    return createNumberMask(cardMask(this.cardType));
  }

  get paymentIcon() {
    if (this.cardType) {
      return (
        <Icon
          name={`fc-payment-${_.kebabCase(this.cardType)}`}
        />
      );
    }
  }

  // Possible values: https://stripe.com/docs/stripe.js?#card-cardType
  get cardType() {
    const { number } = this.props.data;
    const stripeType = foxApi.creditCards.cardType(number);
    return stripeType !== 'Unknown' ? stripeType : void 0;
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

    return foxApi.creditCards.validateCVC(cvc) ? null : t('Please enter a valid cvc number');
  }

  @autobind
  addNew() {
    this.props.resetBillingData();
    this.setState({ addingNew: true });
  }

  @autobind
  cancelEditing() {
    this.props.resetBillingData();
    this.props.clearAddCreditCardErrors();
    this.props.clearUpdateCreditCardErrors();

    if (_.isEmpty(this.props.creditCards)) {
      this.props.togglePaymentModal();
    }

    this.setState({
      addingNew: false,
      cardAdded: false,
    });
  }

  @autobind
  selectCreditCard(creditCard) {
    this.setState({ addingNew: false, selectedCard: creditCard });
  }

  @autobind
  updateCreditCard() {
    const id = _.get(this.props, 'data.id');
    const { billingAddressIsSame } = this.state;

    const operation = id
      ? this.props.updateCreditCard(id, billingAddressIsSame)
      : this.props.addCreditCard(billingAddressIsSame);

    return operation.then((card) => {
      this.props.fetchCreditCards();
      this.props.resetBillingData();
      this.setState({ addingNew: false, cardAdded: (id === undefined) });
      return card;
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

  @autobind
  saveAndContinue() {
    this.props.selectCreditCard(this.state.selectedCard);
    this.props.chooseCreditCard().then(() => {
      this.props.onComplete();
    });
    this.props.togglePaymentModal();
  }

  // Promo Codes Handling
  get addCouponLink() {
    if (!_.isEmpty(this.state.couponCode)) return null;

    const icon = {
      name: 'fc-plus',
      className: styles.plus,
    };

    return (
      <ActionLink
        action={this.addCoupon}
        title="Coupon code"
        icon={icon}
        styleName="action-link-add-methods"
      />
    );
  }

  @autobind
  removeCouponCode() {
    return this.props.removeCouponCode().then(() => {
      this.setState({ couponCode: ''});
    });
  }

  @autobind
  addGC() {
    this.setState({ addingGC: true });
  }

  @autobind
  addCoupon() {
    this.setState({ addingCoupon: true });
  }

  @autobind
  cancelAddingGC() {
    this.setState({ addingGC: false, gcCode: '', error: false });
  }

  @autobind
  cancelAddingCoupon() {
    this.setState({ addingCoupon: false, couponCode: '', error: false });
  }

  @autobind
  onGCChange(code) {
    this.setState({ gcCode: code });
  }

  @autobind
  onCouponChange(code) {
    this.setState({ couponCode: code });
  }

  @autobind
  saveGiftCard() {
    const code = this.state.gcCode.replace(/\s+/g, '');
    this.props.saveGiftCard(code)
      .then(() => this.setState({ gcCode: '', error: false, addingGC: false }))
      .catch((error) => {
        this.setState({ error });
      });
  }

  @autobind
  saveCouponCode() {
    const code = this.state.couponCode.replace(/\s+/g, '');
    this.props.saveCouponCode(code)
      .then(() => this.setState({ error: false, addingCoupon: false }))
      .catch((error) => {
        this.setState({ error });
      });
  }

  // Render Methods
  renderBillingAddress(withoutDefaultCheckbox = false) {
    const { billingAddressIsSame } = this.state;

    if (billingAddressIsSame) {
      const { shippingAddress } = this.props;
      if (_.isEmpty(shippingAddress)) return <div styleName="no-address">Please, enter an address first</div>;

      return (
        <AddressDetails styleName="billing-address" address={shippingAddress} />
      );
    }

    return (
      <div styleName="new-billing-address">
        <EditAddress
          {...this.props}
          withoutDefaultCheckbox={withoutDefaultCheckbox}
          address={this.props.data.address}
          onUpdate={this.props.setBillingAddress}
        />
      </div>
    );
  }

  renderCardEditForm(withoutDefaultCheckbox = false) {
    const { props } = this;
    const { data, t } = props;

    const months = _.range(1, 13, 1).map(x => _.padStart(x.toString(), 2, '0'));
    const currentYear = new Date().getFullYear();
    const years = _.range(currentYear, currentYear + 10, 1).map(x => x.toString());
    const checkedDefaultCard = _.get(data, 'isDefault', false);
    const editingSavedCard = !!data.id;
    const cardNumberPlaceholder = editingSavedCard ?
      (_.repeat('**** ', 3) + data.lastFour) : t('Card Number');
    const cvcPlaceholder = editingSavedCard ? '***' : 'CVC';

    const defaultCheckbox = withoutDefaultCheckbox ? null : (
      <Checkbox
        styleName="default-checkbox"
        name="isDefault"
        checked={checkedDefaultCard}
        onChange={({target}) => this.changeDefault(target.checked)}
        id="set-default-card"
      >
          Set as default
        </Checkbox>
      );

    return (
      <div styleName="card-form">
        <FormField styleName="text-field">
          <TextInput
            required
            pos="top"
            name="holderName"
            placeholder={t('Name on the card')}
            value={data.holderName}
            onChange={this.changeFormData}
          />
        </FormField>
        <div styleName="union-fields">
          <FormField styleName="card-number-field" validator={this.validateCardNumber}>
            <TextInput
              label={this.paymentIcon}
              labelClass={styles['payment-icon']}
              hasCard={!!this.cardType}
              pos="tbl"
            >
              <MaskedInput
                required
                disabled={editingSavedCard}
                styleName="payment-input"
                className={textStyles.textInput}
                type="tel"
                mask={this.cardMask}
                placeholderChar={'\u2000'}
                name="number"
                placeholder={cardNumberPlaceholder}
                value={data.number}
                onChange={this.changeCardNumber}
              />
            </TextInput>
          </FormField>
          <FormField styleName="cvc-field" validator={this.validateCvcNumber}>
            <TextInput
              required
              pos="tbr"
              disabled={editingSavedCard}
              type="number"
              pattern="\d*"
              inputMode="numeric"
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
                placeholder: t('Month'),
                type: 'text',
                pos: 'bl',
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
                placeholder: t('Year'),
                type: 'text',
                pos: 'br',
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
        {defaultCheckbox}
        <Checkbox
          id="billingAddressIsSame"
          checked={this.state.billingAddressIsSame}
          onChange={this.toggleSeparateBillingAddress}
          styleName="same-address-checkbox"
        >
          {t('Billing address is same as shipping')}
        </Checkbox>
        {this.renderBillingAddress(true)}
      </div>
    );
  }

  renderPaymentFeatures() {
    const icon = {
      name: 'fc-plus',
      className: styles.plus,
    };

    return (
      <div key="payment-features" styleName="gc-coupon">
        <PromoCode
          giftCards={this.props.giftCards}
          removeCode={this.props.removeGiftCard}
          styleName="promo-codes"
        />
        <ActionLink
          action={this.addGC}
          title="Gift card"
          icon={icon}
          styleName="action-link-add-methods"
        />

        <PromoCode
          coupon={this.props.coupon}
          removeCode={this.removeCouponCode}
          styleName="promo-codes"
        />
        {this.addCouponLink}
      </div>
    );
  }

  get renderEditPromoForm() {
    if (!this.state.addingGC && !this.state.addingCoupon) return null;

    const isGC = this.state.addingGC;
    const { saveGCProgress, saveCouponProgress } = this.props;

    const title = isGC ? 'Add gift card' : 'Add coupon code';
    const submit = isGC ? this.saveGiftCard : this.saveCouponCode;
    const buttonLabel = isGC ? 'Redeem' : 'Apply';
    const handler = isGC ? this.cancelAddingGC : this.cancelAddingCoupon;
    const action = {
      title: 'Cancel',
      handler,
    };

    const onChange = isGC ? this.onGCChange : this.onCouponChange;
    const placeholder = isGC ? 'Gift card code' : 'Coupon code';
    const inProgress = isGC ? saveGCProgress : saveCouponProgress;

    return (
      <CheckoutForm
        submit={submit}
        title={title}
        error={this.state.error}
        buttonLabel={buttonLabel}
        action={action}
        inProgress={inProgress}
      >
        <EditPromos
          onChange={onChange}
          saveCode={submit}
          placeholder={placeholder}
        />
      </CheckoutForm>
    );
  }

  // main render
  render() {
    const { props } = this;
    const { t, creditCards } = props;

    if (this.state.addingNew || _.isEmpty(creditCards)) {
      const action = {
        handler: this.cancelEditing,
        title: 'Cancel',
      };

      const { data } = props;
      const title = data.id ? t('Edit Card') : t('Add Card');

      return (
        <CheckoutForm
          submit={this.updateCreditCard}
          title={title}
          error={props.updateCreditCardError}
          buttonLabel="Save card"
          action={action}
          inProgress={props.updateCreditCardInProgress}
          buttonDisabled={_.isEmpty(props.shippingAddress) && this.state.billingAddressIsSame}
        >
          {this.renderCardEditForm()}
        </CheckoutForm>
      );
    } else if (this.state.addingGC || this.state.addingCoupon) {
      return this.renderEditPromoForm;
    }

    const action = {
      title: 'Close',
      handler: this.props.togglePaymentModal,
    };

    const icon = {
      name: 'fc-plus',
      className: styles.plus,
    };
    return (
      <CheckoutForm
        submit={this.saveAndContinue}
        title="Payment"
        error={null} // error for placing order action is showed in Checkout component
        buttonLabel="Apply"
        inProgress={props.checkoutState.inProgress}
        action={action}
      >
        <fieldset styleName="credit-cards-list">
          <CreditCards
            creditCards={creditCards}
            selectCreditCard={this.selectCreditCard}
            onEditCard={this.editCard}
            onDeleteCard={this.deleteCreditCard}
            cardAdded={this.state.cardAdded}
            selectedCard={this.state.selectedCard}
          />
          <ActionLink
            action={this.addNew}
            title="Credit card"
            icon={icon}
            styleName="action-link-add-methods"
          />
        </fieldset>
        { this.renderPaymentFeatures() }
      </CheckoutForm>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    data: state.checkout.billingData,
    ...state.cart,
    updateCreditCardError: _.get(state.asyncActions, 'addCreditCard.err')
    || _.get(state.asyncActions, 'updateCreditCard.err'),
    updateCreditCardInProgress: _.get(state.asyncActions, 'addCreditCard.inProgress', false)
    || _.get(state.asyncActions, 'updateCreditCard.inProgress', false),
    checkoutState: _.get(state.asyncActions, 'checkout', {}),
    creditCardsLoading: _.get(state.asyncActions, ['creditCards', 'inProgress'], true),
    creditCards: state.checkout.creditCards,
    saveGCProgress: _.get(state.asyncActions, 'saveGiftCard.inProgress', false),
    saveCouponProgress: _.get(state.asyncActions, 'saveCouponCode.inProgress', false),
  };
};

export default _.flowRight(
  localized,
  connect(mapStateToProps, { ...checkoutActions, ...cartActions })
)(EditBilling);
