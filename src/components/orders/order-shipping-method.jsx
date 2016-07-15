import React, { Element } from 'react';

import ShippingMethod from 'components/shipping/shipping-method';

type Props = {
  order: {
    currentOrder: {
      shippingMethod: Object,
    },
  },
};

const OrderShippingMethod = (props: Props): Element => {
  const { currentOrder } = props.order
  const { shippingMethod } = currentOrder;

  return (
    <ShippingMethod
      currentOrder={currentOrder}
      title="Shipping Method"
      readOnly={true}
      availableShippingMethods={[]}
      shippingMethods={[shippingMethod]} />
  );
};

export default OrderShippingMethod;
