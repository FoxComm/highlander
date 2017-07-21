/* @flow */

import React from 'react';

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
