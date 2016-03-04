/* @flow */

/*
 * Page prototype https://invis.io/EB67L16VZ
 */

import React, { PropTypes } from 'react';
import cssModules from 'react-css-modules';
import styles from './checkout.css';
import { connect } from 'react-redux';

import Icon from '../../common/icon';
import Shipping from './shipping';

import * as actions from '../../../modules/checkout';
import type { CheckoutState, EditStage } from '../../../modules/checkout';

type CheckoutProps = CheckoutState & {
  setEditStage: (stage: EditStage) => Object;
}

const Checkout = (props: CheckoutProps) => {
  const setShippingStage = () => {
    props.setEditStage('shipping');
  };

  const setDeliveryStage = () => {
    props.setEditStage('delivery');
  };

  return (
    <div styleName="checkout">
      <Icon styleName="logo" name="fc-some_brand_logo" />
      <Shipping
        isEditing={props.editStage == 'shipping'}
        editAction={setShippingStage}
        continueAction={setDeliveryStage}
      />
    </div>
  );
};

Checkout.propTypes = {
  children: PropTypes.node,
};

export default connect(state => state.checkout, actions)(cssModules(Checkout, styles));
