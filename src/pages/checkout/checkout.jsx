/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './checkout.css';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';

// components
import Shipping from './01-shipping/shipping';
import Delivery from './02-delivery/delivery';
import Billing from './03-billing/billing';
import OrderSummary from './summary/order-summary';
import Header from './header';

import type { Promise as PromiseType } from 'types/promise';

import * as actions from 'modules/checkout';
import { EditStages } from 'modules/checkout';
import type { CheckoutState, EditStage } from 'modules/checkout';
import { fetch as fetchCart, hideCart } from 'modules/cart';

type Props = CheckoutState & {
  setEditStage: (stage: EditStage) => Object,
  saveShippingAddress: () => PromiseType,
  saveShippingMethod: () => PromiseType,
  setDefaultAddress: () => PromiseType,
  fetchCart: () => PromiseType,
  addCreditCard: () => PromiseType,
  checkout: () => PromiseType,
  hideCart: () => PromiseType,
  addresses: Array<any>,
  fetchAddresses: Function,
  updateAddress: Function,
  cart: Object,
  isAddressLoaded: boolean,
};

class Checkout extends Component {
  props: Props;

  state = {
    isPerformingCheckout: false,
    deliveryInProgress: false,
    shippingInProgress: false,
    error: null,
    isScrolled: false,
  };

  componentDidMount() {
    this.props.fetchCart();
    this.props.hideCart();

    this.checkScroll();
    window.addEventListener('scroll', this.checkScroll);
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
    this.props.setEditStage(EditStages.SHIPPING);
  }

  @autobind
  setDeliveryStage(id) {
    this.performStageTransition('shippingInProgress', () => {
      return this.props.saveShippingAddress(id).then(() => {
        this.props.setEditStage(EditStages.DELIVERY);
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
    this.performStageTransition('isPerformingCheckout', () => {
      return this.props.addCreditCard()
        .then(() => {
          return this.props.setEditStage(EditStages.FINISHED);
        })
        .then(() => {
          return this.props.checkout();
        })
        .then(() => {
          browserHistory.push('/checkout/done');
        });
    });
  }

  errorsFor(stage) {
    if (this.props.editStage === stage) {
      return this.state.error;
    }
  }

  render() {
    const props = this.props;

    return (
      <section styleName="checkout">
        <Header isScrolled={this.state.isScrolled}/>

        <div styleName="content">
          <div styleName="summary">
            <OrderSummary isScrolled={this.state.isScrolled} />
          </div>

          <div styleName="forms">
            <Shipping
              isEditing={props.editStage == EditStages.SHIPPING}
              collapsed={props.editStage < EditStages.SHIPPING}
              editAction={this.setShippingStage}
              inProgress={this.state.shippingInProgress}
              continueAction={this.setDeliveryStage}
              error={this.errorsFor(EditStages.SHIPPING)}
              addresses={this.props.addresses}
              fetchAddresses={this.props.fetchAddresses}
              shippingAddress={_.get(this.props.cart, 'shippingAddress', {})}
              updateAddress={this.props.updateAddress}
              isAddressLoaded={this.props.isAddressLoaded}
            />
            <Delivery
              isEditing={props.editStage == EditStages.DELIVERY}
              editAllowed={props.editStage >= EditStages.DELIVERY}
              collapsed={!props.isDeliveryDirty && props.editStage < EditStages.DELIVERY}
              editAction={this.setDeliveryStage}
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
            />
          </div>
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
    isBillingDirty: isBillingDirty(state),
    isDeliveryDirty: isDeliveryDirty(state),
  };
}

export default connect(mapStateToProps, { ...actions, fetchCart, hideCart })(Checkout);
