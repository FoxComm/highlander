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
import Shipping from './01-shipping/shipping';
import Delivery from './02-delivery/delivery';
import Billing from './03-billing/billing';
import GuestAuth from './05-guest-auth/guest-auth';
import OrderSummary from 'components/order-summary/order-summary';
import Header from './header';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import Loader from 'ui/loader';

// styles
import styles from './checkout.css';

// types
import type { Promise as PromiseType } from 'types/promise';
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
  hideCart: () => PromiseType,
  fetchCart: () => PromiseType,
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
  isScrolled: boolean,
  cart: Object,
}

class Checkout extends Component {
  props: Props;

  state: State = {
    isScrolled: false,
    cart: this.props.cart,
  };

  componentDidMount() {
    this.props.fetchCart().then(() => {
      const { cart } = this.props;
      tracking.checkoutStart(cart.lineItems);

      let editStage = EditStages.SHIPPING;
      if (!_.isEmpty(cart.shippingAddress)) {
        editStage += 1;
      }

      this.props.setEditStage(editStage);
    });
    this.props.hideCart();

    this.checkScroll();
    window.addEventListener('scroll', this.checkScroll);

    if (!this.isEmailSetForCheckout()) {
      this.props.fetchUser();
    }
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.checkScroll);
    this.props.clearCheckoutErrors();
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.cart != this.props.cart) {
      this.setState({
        cart: nextProps.cart,
      });
    }
  }

  checkScroll = () => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
    const checkoutHeaderHeight = 136;
    const isScrolled = scrollTop > checkoutHeaderHeight;

    this.setState({isScrolled});
  };

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
    return this.props.setEditStage(EditStages.DELIVERY);
  }

  @autobind
  setBillingStage() {
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

    const giftCardPresent = _.some(paymentMethods, paymentMethod => {
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
    const { props } = this;
    const isGuestMode = isGuest(_.get(props.auth, 'user'));
    return (
      <div styleName="body">
        <div styleName="summary">
          <OrderSummary
            isScrolled={this.state.isScrolled}
            styleName="summary-content"
            { ...props.cart }
          />
        </div>

        <div styleName="forms">
          <Shipping
            isEditing={props.editStage == EditStages.SHIPPING}
            collapsed={props.editStage < EditStages.SHIPPING}
            editAction={this.setShippingStage}
            onComplete={this.setDeliveryStage}
            addresses={this.props.addresses}
            fetchAddresses={this.props.fetchAddresses}
            shippingAddress={_.get(this.props.cart, 'shippingAddress', {})}
            auth={this.props.auth}
            isGuestMode={isGuestMode}
          />
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
        </div>

        <GuestAuth
          isEditing={!this.isEmailSetForCheckout()}
          continueAction={this.startShipping}
          location={this.props.location}
        />
      </div>
    );
  }

  render() {
    const props = this.props;

    const setStates = {
      setShippingStage: this.setShippingStage,
      setDeliveryStage: this.setDeliveryStage,
      setBillingStage: this.setBillingStage,
    };

    const body = props.fetchCartState.finished ? this.content : <Loader />;

    return (
      <section styleName="checkout">
        <Header
          isScrolled={this.state.isScrolled}
          isGuestAuth={props.editStage == EditStages.GUEST_AUTH}
          currentStage={props.editStage}
          {...setStates}
        />

        <div styleName="content">
          <ErrorAlerts
            sanitizeError={this.sanitizeError}
            error={props.checkoutState.err}
          />
          {body}
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
