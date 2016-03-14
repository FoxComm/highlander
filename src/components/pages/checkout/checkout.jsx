/* @flow */

/*
 * Page prototype https://invis.io/EB67L16VZ
 */

import React, { PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './checkout.css';
import { connect } from 'react-redux';

import Icon from 'ui/icon';
import Shipping from './shipping';
import Delivery from './delivery';

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
  }

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
      </div>
    </div>
  );
};

Checkout.propTypes = {
  children: PropTypes.node,
};

export default connect(state => state.checkout, actions)(cssModules(Checkout, styles));
