import React, { Element } from 'react';

import ShippingMethod from 'components/shipping/shipping-method';

type Props = {
  order: {
    shippingMethod: Object,
  },
};

const OrderShippingMethod = (props: Props) => {
  const { order } = props;
  const { shippingMethod } = order;

  return (
    <ShippingMethod
      currentOrder={order}
      title="Shipping Method"
      readOnly={true}
      availableShippingMethods={[]}
      isEditing={false}
      shippingMethods={[shippingMethod]} />
  );
};

export default OrderShippingMethod;
