'use strict';

import React from 'react';
import ShippingMethod from '../shipping/shipping-method';

const OrderShippingMethod = (props) => {
  const shippingMethod = props.order.currentOrder.shippingMethod;
  
  return (
    <ShippingMethod
      shippingMethods={[shippingMethod]}
      isEditing={false}
      editAction={() => console.log('Not implemented')}
      doneAction={() => console.log('Not implemented')} />
  );
};

export default OrderShippingMethod;
