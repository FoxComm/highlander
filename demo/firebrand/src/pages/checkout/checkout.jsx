/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './checkout.css';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';
import { Link } from 'react-router';

import Icon from 'ui/icon';
import Shipping from './shipping';
import Delivery from './delivery';
import Billing from './billing';
import OrderSummary from './order-summary';
import GiftCard from './gift-card';
import CouponCode from './coupon-code';

import type { Promise as PromiseType } from 'types/promise';

import * as actions from 'modules/checkout';
import { EditStages } from 'modules/checkout';
import type { CheckoutState, EditStage } from 'modules/checkout';
import { fetch as fetchCart, hideCart } from 'modules/cart';

type CheckoutProps = CheckoutState & {
  setEditStage: (stage: EditStage) => Object;
  saveShippingAddress: () => PromiseType;
  saveShippingMethod: () => PromiseType;
  fetchCart: () => PromiseType;
  addCreditCard: () => PromiseType;
  checkout: () => PromiseType;
  hideCart: () => PromiseType;
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
    isBillingDirty: isBillingDirty(state),
    isDeliveryDirty: isDeliveryDirty(state),
  };
}

class Checkout extends Component {
  props: CheckoutProps;

  state = {
    isPerformingCheckout: false,
    deliveryInProgress: false,
    shippingInProgress: false,
    error: null,
  };

  componentDidMount() {
    this.props.fetchCart();
    this.props.hideCart();
  }

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
  setDeliveryStage() {
    this.performStageTransition('shippingInProgress', () => {
      return this.props.saveShippingAddress().then(() => {
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
      <div styleName="checkout">
        <div styleName="logo-link">
          <Link to="/">
            <Icon styleName="logo" name="fc-some_brand_logo"/>
          </Link>
        </div>
        <div styleName="checkout-content">
          <div styleName="left-forms">
            <Shipping
              isEditing={props.editStage == EditStages.SHIPPING}
              collapsed={props.editStage < EditStages.SHIPPING}
              editAction={this.setShippingStage}
              inProgress={this.state.shippingInProgress}
              continueAction={this.setDeliveryStage}
              error={this.errorsFor(EditStages.SHIPPING)}
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
          <div styleName="right-forms">
            <OrderSummary />
            <CouponCode />
            <GiftCard />
          </div>
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, { ...actions, fetchCart, hideCart })(Checkout);
