/* @flow */

// libs
import React from 'react';
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
    <div styleName="selected">
      <div styleName="name">{shippingMethod.name}</div>
      <div styleName="price">{props.shippingMethodCost(shippingMethod.price)}</div>
    </div>
  );
};

export default connect(state => state.cart)(ViewDelivery);
