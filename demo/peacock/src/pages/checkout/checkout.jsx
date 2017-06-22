/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'lib/history';
import * as tracking from 'lib/analytics';
import { emailIsSet, isGuest } from 'paragons/auth';
import classNames from 'classnames';

// components
import Shipping from './shipping/shipping';
import Delivery from './delivery/delivery';
import Billing from './billing/billing';
import GuestAuth from './guest-auth/guest-auth';
import Products from 'components/order-summary/product-table';
import ErrorAlerts from 'ui/alerts/error-alerts';
import Loader from 'ui/loader';
import OrderTotals from 'components/order-summary/totals';
import Button from 'ui/buttons';

// styles
import styles from './checkout.css';

// types
import type { CheckoutState } from 'modules/checkout';
import type { CheckoutActions } from './types';
import type { AsyncStatus } from 'types/async-actions';

// actions
import * as actions from 'modules/checkout';
import { fetch as fetchCart, hideCart } from 'modules/cart';
import { fetchUser } from 'modules/auth';


type Props = CheckoutState & CheckoutActions & {
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
  isContentOverlayVisible: boolean,
};

type State = {
  shippingDone: boolean,
  deliveryDone: boolean,
  billingDone: boolean,
}

class Checkout extends Component {
  props: Props;

  state: State = {
    shippingDone: false,
    deliveryDone: false,
    billingDone: false,
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
    const { creditCard, fetchCartState } = nextProps;
    const { shippingAddress, shippingMethod, skus } = nextProps.cart;
    const { billingDone, shippingDone, deliveryDone } = this.state;

    if (_.isEmpty(skus) && fetchCartState.finished) {
      browserHistory.push('/');
      return;
    }

    let billing = billingDone;
    let shipping = shippingDone;
    let delivery = deliveryDone;

    if (_.isEmpty(creditCard) && billingDone) {
      billing = false;
    } else if (!_.isEmpty(creditCard) && !billingDone) {
      billing = true;
    }

    if (_.isEmpty(shippingAddress) && shippingDone) {
      shipping = false;
    } else if (!_.isEmpty(shippingAddress) && !shippingDone) {
      shipping = true;
    }

    if (_.isEmpty(shippingMethod) && deliveryDone) {
      delivery = false;
    } else if (!_.isEmpty(shippingMethod) && !deliveryDone) {
      delivery = true;
    }

    this.setState({ billingDone: billing, shippingDone: shipping, deliveryDone: delivery });
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
  setShipping() {
    this.props.toggleShippingModal();
    this.setState({ shippingDone: true });
  }

  @autobind
  setDelivery() {
    this.props.toggleDeliveryModal();
    this.setState({ deliveryDone: true });
  }

  @autobind
  setBilling() {
    this.setState({ billingDone: true });
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
        browserHistory.push('/checkout/done');
      });
  }

  @autobind
  isEmailSetForCheckout() {
    const user = _.get(this.props, ['auth', 'user'], null);
    return emailIsSet(user);
  }

  get orderTotals() {
    const { cart } = this.props;
    const { billingDone, shippingDone, deliveryDone } = this.state;
    const disabled = billingDone && shippingDone && deliveryDone;

    return (
      <div styleName="total-cost">
        <div styleName="totals-list">
          <OrderTotals
            totals={cart.totals}
            paymentMethods={cart.paymentMethods}
          />
        </div>

        <div styleName="place-order-block">
          <Button
            styleName="place-order-button"
            onClick={this.placeOrder}
            disabled={!disabled}
            isLoading={this.props.checkoutState.inProgress}
          >
            Place order
          </Button>
        </div>
      </div>
    );
  }

  get orderContent() {
    return (
      <div styleName="order-content">
        <Products skus={this.props.cart.skus} />
      </div>
    );
  }

  get content() {
    const { props } = this;
    const isGuestMode = isGuest(_.get(props.auth, 'user'));
    const cartFetched = props.fetchCartState.finished;

    if (cartFetched) {
      const shippingAddress = _.get(this.props.cart, 'shippingAddress', {});
      console.log('billing', this.props);

      return (
        <div styleName="wrapper">
          <div styleName="main-container">
            <div styleName="row">
              <div styleName="column">
                <div styleName="shipping">
                  <Shipping
                    isEditing={props.editStage}
                    onComplete={this.setShipping}
                    addresses={this.props.addresses}
                    fetchAddresses={this.props.fetchAddresses}
                    fetchAddressesRequired={this.props.fetchAddressesRequired}
                    shippingAddress={shippingAddress}
                    auth={this.props.auth}
                    isGuestMode={isGuestMode}
                  />
                </div>
                <div styleName="delivery">
                  <Delivery
                    isEditing={props.editStage}
                    onComplete={this.setDelivery}
                    shippingMethods={props.shippingMethods}
                    cart={this.props.cart}
                    fetchShippingMethods={props.fetchShippingMethods}
                    shippingAddressEmpty={_.isEmpty(shippingAddress)}
                  />
                </div>
              </div>
              <div styleName="payment">
                <Billing
                  isGuestMode={isGuestMode}
                  paymentMethods={_.get(props.cart, 'paymentMethods', [])}
                  chooseCreditCard={this.props.chooseCreditCard}
                  onComplete={this.setBilling}
                />
              </div>
            </div>
            <div styleName="order-summary">
              {this.orderContent}
            </div>
          </div>
          <div styleName="side-container">
            {this.orderTotals}
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

    const overlayClass = classNames(styles['content-container-overlay'], {
      [styles['_with-overlay']]: props.isContentOverlayVisible,
    });

    return (
      <section styleName="checkout">
        <div styleName="content">
          <ErrorAlerts
            sanitizeError={this.sanitizeError}
            error={props.checkoutState.err}
          />
          {this.content}
        </div>

        <div className={overlayClass} />
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
