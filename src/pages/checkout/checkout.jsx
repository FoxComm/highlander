/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';

// components
import Shipping from './01-shipping/shipping';
import Delivery from './02-delivery/delivery';
import Billing from './03-billing/billing';
import GuestAuth from './05-guest-auth/guest-auth';
import OrderSummary from '../../components/order-summary/order-summary';
import Header from './header';

// styles
import styles from './checkout.css';

// types
import type { Promise as PromiseType } from 'types/promise';
import type { CheckoutState, EditStage } from 'modules/checkout';
import type { CheckoutActions } from './types';

// actions
import * as actions from 'modules/checkout';
import { EditStages } from 'modules/checkout';
import { fetch as fetchCart, hideCart } from 'modules/cart';
import { fetchUser } from 'modules/auth';

// paragons
import { emailIsSet } from 'paragons/auth';

type Props = CheckoutState & CheckoutActions & {
  setEditStage: (stage: EditStage) => Object,
  hideCart: () => PromiseType,
  fetchCart: () => PromiseType,
  addresses: Array<any>,
  shippingMethods: Object,
  cart: Object,
  location: Object,
};

class Checkout extends Component {
  props: Props;

  state = {
    isPerformingCheckout: false,
    deliveryInProgress: false,
    shippingInProgress: false,
    billingInProgress: false,
    isProceedingCard: false,
    guestAuthInProgress: false,
    error: null,
    isScrolled: false,
  };

  componentDidMount() {
    this.props.fetchCart();
    this.props.hideCart();

    this.checkScroll();
    window.addEventListener('scroll', this.checkScroll);

    if (!this.isEmailSetForCheckout()) {
      this.props.fetchUser();
    }
  }

  componentWillUnmount() {
    window.removeEventListener('scroll', this.checkScroll);
  }

  checkScroll = () => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
    const checkoutHeaderHeight = 136;
    const isScrolled = scrollTop > checkoutHeaderHeight;

    this.setState({isScrolled});
  };

  @autobind
  performStageTransition(name: string, perform: () => PromiseType): PromiseType {
    return new Promise(resolve => {
      this.setState({
        [name]: true,
        error: null,
      }, () => {
        perform().then(
          () => {
            this.setState({
              [name]: false,
            }, resolve);
          },
          err => {
            this.setState({
              [name]: false,
              error: err,
            }, resolve);
          }
        );
      });
    });
  }

  @autobind
  setShippingStage() {
    this.setState({error: null});
    this.props.setEditStage(EditStages.SHIPPING);
  }

  @autobind
  setDeliveryStage() {
    this.setState({error: null});
    return this.props.setEditStage(EditStages.DELIVERY);
  }

  @autobind
  saveShippingAddress(id) {
    this.performStageTransition('shippingInProgress', () => {
      return this.props.saveShippingAddress(id).then(() => {
        this.setDeliveryStage();
      });
    });
  }

  @autobind
  setBillingState() {
    this.performStageTransition('deliveryInProgress', () => {
      return this.props.saveShippingMethod().then(() => {
        this.props.setEditStage(EditStages.BILLING);
      });
    });
  }

  @autobind
  placeOrder() {
    if (this.props.cart.creditCard) {
      return this.props.chooseCreditCard()
        .then(() => this.checkout());
    }

    return this.checkout();
  }

  @autobind
  checkout() {
    this.performStageTransition('isPerformingCheckout', () => {
      return this.props.checkout()
        .then(() => {
          this.props.setEditStage(EditStages.FINISHED);
          browserHistory.push('/checkout/done');
        });
    });
  }

  @autobind
  proceedCreditCard(billingAddressIsSame, id) {
    return this.performStageTransition('isProceedingCard', () => {
      return id
        ? this.props.updateCreditCard(id, billingAddressIsSame)
        : this.props.addCreditCard(billingAddressIsSame)
        .then(() => {
          this.props.setEditStage(EditStages.BILLING);
        });
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
  checkoutAfterSignIn() {
    this.props.updateAddress().then(() => {
      return this.saveShippingAddress();
    }).then(() => {
      return this.props.saveShippingMethod();
    }).then(() => {
      return this.placeOrder();
    });
  }

  errorsFor(stage) {
    if (this.props.editStage === stage) {
      return this.state.error;
    }
  }

  render() {
    const props = this.props;

    const setStates = {
      setShippingStage: this.setShippingStage,
      setDeliveryStage: this.setDeliveryStage,
      setBillingState: this.setBillingState,
    };

    return (
      <section styleName="checkout">
        <Header
          isScrolled={this.state.isScrolled}
          isGuestAuth={props.editStage == EditStages.GUEST_AUTH}
          {...setStates}
        />

        <div styleName="content">
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
              inProgress={this.state.shippingInProgress}
              continueAction={this.saveShippingAddress}
              error={this.errorsFor(EditStages.SHIPPING)}
              addresses={this.props.addresses}
              fetchAddresses={this.props.fetchAddresses}
              shippingAddress={_.get(this.props.cart, 'shippingAddress', {})}
              updateAddress={this.props.updateAddress}
              auth={this.props.auth}
            />
            <Delivery
              isEditing={props.editStage == EditStages.DELIVERY}
              editAllowed={props.editStage >= EditStages.DELIVERY}
              collapsed={!props.isDeliveryDirty && props.editStage < EditStages.DELIVERY}
              editAction={this.setDeliveryStage}
              shippingMethods={props.shippingMethods}
              selectedShippingMethod={props.cart.shippingMethod}
              fetchShippingMethods={props.fetchShippingMethods}
              inProgress={this.state.deliveryInProgress}
              continueAction={this.setBillingState}
              error={this.errorsFor(EditStages.DELIVERY)}
            />
            <Billing
              isEditing={props.editStage == EditStages.BILLING}
              editAllowed={props.editStage >= EditStages.BILLING}
              collapsed={!props.isBillingDirty && props.editStage < EditStages.BILLING}
              editAction={this.setBillingState}
              inProgress={this.state.isPerformingCheckout}
              continueAction={this.placeOrder}
              error={this.errorsFor(EditStages.BILLING)}
              paymentMethods={_.get(props.cart, 'paymentMethods', [])}
              proceedCreditCard={this.proceedCreditCard}
              performStageTransition={this.performStageTransition}
            />
          </div>

          <GuestAuth
            isEditing={!this.isEmailSetForCheckout()}
            inProgress={this.state.guestAuthInProgress}
            error={this.errorsFor(EditStages.GUEST_AUTH)}
            continueAction={this.startShipping}
            checkoutAfterSignIn={this.startShipping}
            location={this.props.location}
          />
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
  };
}

export default connect(mapStateToProps, { ...actions, fetchCart, hideCart, fetchUser })(Checkout);
