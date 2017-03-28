/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'lib/history';
import * as tracking from 'lib/analytics';
import { emailIsSet, isGuest } from 'paragons/auth';

// components
import Shipping from './shipping/shipping';
import Delivery from './delivery/delivery';
// import Billing from './billing/billing';
import GuestAuth from './guest-auth/guest-auth';
// import OrderSummary from 'components/order-summary/order-summary';
import Header from 'components/header/header';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import Loader from 'ui/loader';

// styles
import styles from './checkout.css';

// types
import type { CheckoutState, EditStage } from 'modules/checkout';
import type { CheckoutActions } from './types';
import type { AsyncStatus } from 'types/async-actions';

// actions
import * as actions from 'modules/checkout';
import { EditStages } from 'modules/checkout';
import { fetch as fetchCart, hideCart } from 'modules/cart';
import { fetchUser } from 'modules/auth';


type Props = CheckoutState & CheckoutActions & {
  setEditStage: (stage: EditStage) => Object,
  hideCart: () => Promise<*>,
  fetchCart: () => Promise<*>,
  addresses: Array<any>,
  shippingMethods: Object,
  cart: Object,
  isAddressLoaded: boolean,
  location: Object,
  fetchCartState: AsyncStatus,
  checkoutState: AsyncStatus,
  clearCheckoutErrors: () => void,
};

type State = {
  cart: Object,
}

class Checkout extends Component {
  props: Props;

  state: State = {
    cart: this.props.cart,
  };

  componentDidMount() {
    this.props.fetchCart().then(() => {
      const { cart } = this.props;
      tracking.checkoutStart(cart.lineItems);
    });

    if (!this.isEmailSetForCheckout()) {
      this.props.fetchUser();
    }
  }

  componentWillUnmount() {
    this.props.clearCheckoutErrors();
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.cart != this.props.cart) {
      this.setState({
        cart: nextProps.cart,
      });
    }
  }

  @autobind
  sanitizeError(error) {
    if (error && error.startsWith('Not enough onHand units')) {
      return 'Unable to checkout — item is out of stock';
    } else if (/is blacklisted/.test(error)) {
      return 'Your account has been blocked from making purchases on this site';
    }

    return error;
  }

  @autobind
  setShippingStage() {
    this.props.setEditStage(EditStages.SHIPPING);
  }

  @autobind
  setDeliveryStage() {
    this.props.toggleShippingModal();
    return this.props.setEditStage(EditStages.DELIVERY);
  }

  @autobind
  setBillingStage() {
    this.props.toggleDeliveryModal();
    return this.props.setEditStage(EditStages.BILLING);
  }

  @autobind
  placeOrder() {
    const { creditCard } = this.props;
    const { paymentMethods } = this.props.cart;
    if (creditCard) {
      tracking.chooseBillingMethod(creditCard.brand);
      return this.props.chooseCreditCard()
        .then(() => this.checkout());
    }

    const giftCardPresent = _.some(paymentMethods, (paymentMethod) => {
      return paymentMethod.type == 'giftCard';
    });

    if (giftCardPresent) {
      tracking.chooseBillingMethod('GiftCard');
    }

    return this.checkout();
  }

  @autobind
  checkout() {
    return this.props.checkout()
      .then(() => {
        this.props.setEditStage(EditStages.FINISHED);
        browserHistory.push('/checkout/done');
      });
  }

  @autobind
  startShipping() {
    return this.props.setEditStage(EditStages.SHIPPING);
  }

  @autobind
  isEmailSetForCheckout() {
    const user = _.get(this.props, ['auth', 'user'], null);
    return emailIsSet(user);
  }

  @autobind
  handleUpdateCart(cart) {
    this.setState({
      cart,
    });
  }

  get content() {
    /* elements to be added later
    <div styleName="summary">
      <OrderSummary
        isScrolled={this.state.isScrolled}
        styleName="summary-content"
        { ...props.cart }
      />
    </div>

    <Delivery
      isEditing={props.editStage == EditStages.DELIVERY}
      editAllowed={props.editStage >= EditStages.DELIVERY}
      collapsed={!props.isDeliveryDirty && props.editStage < EditStages.DELIVERY}
      editAction={this.setDeliveryStage}
      onComplete={this.setBillingStage}
      shippingMethods={props.shippingMethods}
      cart={this.state.cart}
      onUpdateCart={this.handleUpdateCart}
      fetchShippingMethods={props.fetchShippingMethods}
    />
    <Billing
      isGuestMode={isGuestMode}
      isEditing={props.editStage == EditStages.BILLING}
      editAllowed={props.editStage >= EditStages.BILLING}
      collapsed={!props.isBillingDirty && props.editStage < EditStages.BILLING}
      editAction={this.setBillingStage}
      continueAction={this.placeOrder}
      paymentMethods={_.get(props.cart, 'paymentMethods', [])}
    />


    <GuestAuth
      isEditing={!this.isEmailSetForCheckout()}
      continueAction={this.startShipping}
      location={this.props.location}
    />
    */
    const { props } = this;
    const isGuestMode = isGuest(_.get(props.auth, 'user'));
    const cartFetched = props.fetchCartState.finished;

    if (cartFetched) {
      return (
        <div styleName="wrapper">
          <div styleName="shipping">
            <Shipping
              isEditing={props.editStage}
              editAction={this.setShippingStage}
              onComplete={this.setDeliveryStage}
              addresses={this.props.addresses}
              fetchAddresses={this.props.fetchAddresses}
              shippingAddress={_.get(this.props.cart, 'shippingAddress', {})}
              auth={this.props.auth}
              isGuestMode={isGuestMode}
            />
          </div>
          <div styleName="delivery">
            <Delivery
              isEditing={props.editStage}
              editAction={this.setDeliveryStage}
              onComplete={this.setBillingStage}
              shippingMethods={props.shippingMethods}
              cart={this.state.cart}
              onUpdateCart={this.handleUpdateCart}
              fetchShippingMethods={props.fetchShippingMethods}
            />
          </div>

          <GuestAuth
            isEditing={!this.isEmailSetForCheckout()}
            location={this.props.location}
          />
        </div>
      );
    }

    return (
      <Loader />
    );
  }

  render() {
    const props = this.props;

    return (
      <section styleName="checkout">
        <Header
          path={props.location.pathname}
          query={props.location.query}
        />

        <div styleName="content">
          <ErrorAlerts
            sanitizeError={this.sanitizeError}
            error={props.checkoutState.err}
          />
          {this.content}
        </div>
      </section>
    );
  }
}

function isDeliveryDirty(state) {
  return !!state.cart.shippingMethod;
}

function isBillingDirty(state) {
  return !_.isEmpty(state.checkout.billingData) || !_.isEmpty(state.checkout.billingAddress);
}


function mapStateToProps(state) {
  return {
    ...state.checkout,
    cart: state.cart,
    auth: state.auth,
    isBillingDirty: isBillingDirty(state),
    isDeliveryDirty: isDeliveryDirty(state),
    fetchCartState: _.get(state.asyncActions, 'cart', {}),
    checkoutState: _.get(state.asyncActions, 'checkout', {}),
  };
}

export default connect(mapStateToProps, { ...actions, fetchCart, hideCart, fetchUser })(Checkout);
