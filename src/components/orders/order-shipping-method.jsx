import React from 'react';
import ShippingMethod from '../shipping/shipping-method';

const OrderShippingMethod = props => {
  const shippingMethod = props.order.currentOrder.shippingMethod;

  return (
    <ShippingMethod
      currentOrder={props.order.currentOrder}
      availableShippingMethods={props.shippingMethods.availableMethods}
      shippingMethods={[shippingMethod]}
      isEditing={props.shippingMethods.isEditing}
      editAction={() => props.fetchShippingMethods(props.order.currentOrder)}
      doneAction={props.orderShippingMethodCancelEdit}
      updateAction={props.updateShippingMethod}
      isEditingPrice={props.shippingMethods.isEditingPrice}
      editPriceAction={props.orderShippingMethodStartEditPrice}
      cancelPriceAction={props.orderShippingMethodCancelEditPrice} />
  );
};

export default OrderShippingMethod;
