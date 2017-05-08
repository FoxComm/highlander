/* @flow */

import React from 'react';

import _ from 'lodash';

// styles
import styles from './delivery.css';

type Props = {
  shippingMethod: Object,
  shippingMethodCost: (cost: number) => React$Element<*>,
  shippingMethodsEmpty: boolean,
  shippingAddressEmpty: boolean,
  loadingShippingMethods: boolean,
};

const ViewDelivery = (props: Props) => {
  const { shippingMethod, shippingMethodsEmpty, shippingAddressEmpty, loadingShippingMethods } = props;

  if (shippingMethodsEmpty && !shippingAddressEmpty && !loadingShippingMethods) {
    return (
      <div styleName="helpful-hints">
        There&#39;s no shipping to this address, please choose another one.
      </div>
    );
  }

  if (shippingAddressEmpty) {
    return (
      <div styleName="helpful-hints">
        Choose shipping address first!
      </div>
    );
  }

  if (_.isEmpty(shippingMethod)) return null;

  return (
    <div styleName="delivery">
      <div styleName="method">{shippingMethod.name}</div>
      <div styleName="cost">{props.shippingMethodCost(shippingMethod.price)}</div>
    </div>
  );
};

export default ViewDelivery;
