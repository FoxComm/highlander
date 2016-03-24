/* @flow */

/*
 * Page prototype https://invis.io/EB67L16VZ
 */

import _ from 'lodash';
import React from 'react';
import styles from './checkout.css';
import { connect } from 'react-redux';

import Icon from 'ui/icon';
import Shipping from './shipping';
import Delivery from './delivery';
import Billing from './billing';
import OrderSummary from './order-summary';
import GiftCard from './gift-card';

import * as actions from 'modules/checkout';
import { EditStages } from 'modules/checkout';
import type { CheckoutState, EditStage } from 'modules/checkout';

type CheckoutProps = CheckoutState & {
  setEditStage: (stage: EditStage) => Object;
}

function isDeliveryDurty(state) {
  return !!state.checkout.selectedShippingMethod;
}

function isBillingDurty(state) {
  return !_.isEmpty(state.checkout.billingData) || !_.isEmpty(state.checkout.billingAddress);
}

function mapStateToProps(state) {
  return {
    ...state.checkout,
    isBillingDurty: isBillingDurty(state),
    isDeliveryDurty: isDeliveryDurty(state),
  };
}

const Checkout = (props: CheckoutProps) => {
  const setShippingStage = () => {
    props.setEditStage(EditStages.SHIPPING);
  };

  const setDeliveryStage = () => {
    props.setEditStage(EditStages.DELIVERY);
  };

  const setBillingState = () => {
    props.setEditStage(EditStages.BILLING);
  };

  const placeOrder = () => {
    console.info('TODO: place order');
  };

  return (
    <div styleName="checkout">
      <Icon styleName="logo" name="fc-some_brand_logo" />
      <div styleName="checkout-content">
        <div styleName="left-forms">
          <Shipping
            isEditing={props.editStage == EditStages.SHIPPING}
            collapsed={props.editStage < EditStages.SHIPPING}
            editAction={setShippingStage}
            continueAction={setDeliveryStage}
          />
          <Delivery
            isEditing={props.editStage == EditStages.DELIVERY}
            editAllowed={props.editStage >= EditStages.DELIVERY}
            collapsed={!props.isDeliveryDurty && props.editStage < EditStages.DELIVERY}
            editAction={setDeliveryStage}
            continueAction={setBillingState}
          />
          <Billing
            isEditing={props.editStage == EditStages.BILLING}
            editAllowed={props.editStage >= EditStages.BILLING}
            collapsed={!props.isBillingDurty && props.editStage < EditStages.BILLING}
            editAction={setBillingState}
            continueAction={placeOrder}
          />
        </div>
        <div styleName="right-forms">
          <OrderSummary />
          <GiftCard />
        </div>
      </div>
    </div>
  );
};

export default connect(mapStateToProps, actions)(Checkout);
