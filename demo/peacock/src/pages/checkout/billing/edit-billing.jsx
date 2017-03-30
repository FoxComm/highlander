/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import textStyles from 'ui/text-input/text-input.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { cardMask } from '@foxcomm/wings/lib/payment-cards';
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
import CvcHelp from './cvc-help';
import PromoCode from 'components/promo-code/promo-code';
import CheckoutForm from '../checkout-form';
import Accordion from 'components/accordion/accordion';
import Loader from 'ui/loader';
import ActionLink from 'ui/action-link/action-link';
import { AddressDetails } from 'ui/address';

// styles
import styles from './billing.css';
// $FlowFixMe: there is style name from css module
import { subtitle } from '../shipping/guest-shipping.css';

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
  saveCouponCode: Function,
  removeCouponCode: Function,
  coupon: ?Object,
  promotion: ?Object,
  totals: Object,
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
  };

  componentWillMount() {
    if (!this.props.isGuestMode) {
      const chosenCreditCard = _.find(this.props.paymentMethods, {type: 'creditCard'});
      if (chosenCreditCard) {
        this.props.selectCreditCard(chosenCreditCard);
      }
    }

    if (this.props.data.address) {
      this.state.billingAddressIsSame = false;
    }
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

  renderBillingAddress(withoutDefaultCheckbox = false) {
    const { billingAddressIsSame } = this.state;

    if (billingAddressIsSame) {
      const { shippingAddress } = this.props;
      return (
        <AddressDetails styleName="billing-address" address={shippingAddress} />
      );
    }

    return (
      <EditAddress
        {...this.props}
        withoutDefaultCheckbox={withoutDefaultCheckbox}
        address={this.props.data.address}
        onUpdate={this.props.setBillingAddress}
      />
    );
  }

  // Possible values: https://stripe.com/docs/stripe.js?#card-cardType
  get cardType() {
    const { number } = this.props.data;
    const stripeType = foxApi.creditCards.cardType(number);
    return stripeType !== 'Unknown' ? stripeType : void 0;
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
    this.setState({
      addingNew: false,
      cardAdded: false,
    });
    this.props.togglePaymentModal();
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
              label={<CvcHelp />}
              labelClass={styles['cvc-icon']}
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
                pos:'br',
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
    return (
      <div key="payment-features" styleName="gc-coupon">
        <Accordion title="COUPON CODE?">
          <PromoCode
            placeholder="Coupon Code"
            coupon={this.props.coupon}
            promotion={this.props.promotion}
            discountValue={this.props.totals.adjustments}
            saveCode={this.props.saveCouponCode}
            removeCode={this.props.removeCouponCode}
            context="billingEdit"
          />
        </Accordion>

        <Accordion title="GIFT CARD?">
          <PromoCode
            placeholder="Gift Card Number"
            buttonLabel="Redeem"
            giftCards={this.props.giftCards}
            saveCode={this.props.saveGiftCard}
            removeCode={this.props.removeGiftCard}
            context="billingEdit"
          />
        </Accordion>
      </div>
    );
  }

  @autobind
  submitCardAndContinue() {
    return this.updateCreditCard().then((card) => {
      this.props.selectCreditCard(card);
    });
  }

  @autobind
  saveAndContinue() {
    this.props.selectCreditCard(this.state.selectedCard);
    this.props.chooseCreditCard();
    this.props.togglePaymentModal();
  }

  renderGuestView() {
    const { props } = this;

    return (
      <CheckoutForm
        submit={this.submitCardAndContinue}
        error={props.updateCreditCardError}
        buttonLabel="Place Order"
        inProgress={props.updateCreditCardInProgress || props.checkoutState.inProgress}
      >
        <div className={subtitle}>PAYMENT METHOD</div>
        {this.renderCardEditForm(true)}
        { this.renderPaymentFeatures() }
      </CheckoutForm>
    );
  }

  render() {
    const { props } = this;
    const { t, creditCardsLoading, creditCards } = props;

    if (props.isGuestMode) {
      return this.renderGuestView();
    }

    if (creditCardsLoading) {
      return <Loader size="m" />;
    }

    // Explicitly show card form if user doesn't have any cards
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
          buttonLabel="Save Card"
          action={action}
          inProgress={props.updateCreditCardInProgress}
        >
          {this.renderCardEditForm()}
        </CheckoutForm>
      );
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
            title="Add card"
            icon={icon}
            styleName="action-link-add-card"
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
  };
};

export default _.flowRight(
  localized,
  connect(mapStateToProps, { ...checkoutActions, ...cartActions })
)(EditBilling);
