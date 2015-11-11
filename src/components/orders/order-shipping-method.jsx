import React from 'react';
import ShippingMethod from '../shipping/shipping-method';

const OrderShippingMethod = props => {
  const shippingMethod = props.order.currentOrder.shippingMethod;

  return (
    <ShippingMethod
      availableShippingMethods={props.shippingMethods.availableMethods}
      shippingMethods={[shippingMethod]}
      isEditing={props.shippingMethods.isEditing}
      editAction={() => props.fetchShippingMethods(props.order.currentOrder)}
      doneAction={props.orderShippingMethodCancelEdit}
      isEditingPrice={props.shippingMethods.isEditingPrice}
      editPriceAction={props.orderShippingMethodStartEditPrice}
      cancelPriceAction={props.orderShippingMethodCancelEditPrice} />
  );
};

export default OrderShippingMethod;
