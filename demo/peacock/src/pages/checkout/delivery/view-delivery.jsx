/* @flow */

import React from 'react';

// libs
import { connect } from 'react-redux';

// styles
import styles from './delivery.css';

type Props = {
  shippingMethod: ?Object,
  shippingMethodCost: Function,
};

const ViewDelivery = (props: Props) => {
  const { shippingMethod } = props;

  if (!shippingMethod) return null;

  return (
    <div styleName="delivery">
      <div styleName="method">{shippingMethod.name}</div>
      <div styleName="cost">{props.shippingMethodCost(shippingMethod.price)}</div>
    </div>
  );
};

export default ViewDelivery;
