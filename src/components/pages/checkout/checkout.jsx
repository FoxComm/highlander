/* @flow */

/*
 * Page prototype https://invis.io/EB67L16VZ
 */

import React, { PropTypes } from 'react';
import styles from './checkout.css';
import { connect } from 'react-redux';

import Icon from 'ui/icon';
import Shipping from './shipping';
import Delivery from './delivery';
import Billing from './billing';

import * as actions from 'modules/checkout';
import { EditStages } from 'modules/checkout';
import type { CheckoutState, EditStage } from 'modules/checkout';

type CheckoutProps = CheckoutState & {
  setEditStage: (stage: EditStage) => Object;
}

const Checkout = (props: CheckoutProps) => {
  const setShippingStage = () => {
    props.setEditStage(EditStages.shipping);
  };

  const setDeliveryStage = () => {
    props.setEditStage(EditStages.delivery);
  };

  const setBillingState = () => {
    props.setEditStage(EditStages.billing);
  };

  const placeOrder = () => {
    console.info('TODO: place order');
  };

  return (
    <div styleName="checkout">
      <Icon styleName="logo" name="fc-some_brand_logo" />
      <div styleName="left-forms">
        <Shipping
          isEditing={props.editStage == EditStages.shipping}
          collapsed={props.editStage < EditStages.shipping}
          editAction={setShippingStage}
          continueAction={setDeliveryStage}
        />
        <Delivery
          isEditing={props.editStage == EditStages.delivery}
          collapsed={props.editStage < EditStages.delivery}
          editAction={setDeliveryStage}
          continueAction={setBillingState}
        />
        <Billing
          isEditing={props.editStage == EditStages.billing}
          collapsed={props.editStage < EditStages.billing}
          editAction={setBillingState}
          continueAction={placeOrder}
        />
      </div>
    </div>
  );
};

Checkout.propTypes = {
  children: PropTypes.node,
};

export default connect(state => state.checkout, actions)(Checkout);
