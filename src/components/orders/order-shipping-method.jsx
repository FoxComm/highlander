import React, { PropTypes } from 'react';
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

OrderShippingMethod.propTypes = {
  order: PropTypes.shape({
    currentOrder: PropTypes.shape({
      shippingMethod: PropTypes.object
    })
  }).isRequired,
  shippingMethods: PropTypes.shape({
    availableMethods: PropTypes.array,
    isEditing: PropTypes.bool.isRequired,
    isEditingPrice: PropTypes.bool
  }),
  orderShippingMethodCancelEdit: PropTypes.func,
  orderShippingMethodStartEditPrice: PropTypes.func,
  orderShippingMethodCancelEditPrice: PropTypes.func,
  updateShippingMethod: PropTypes.func
};


export default OrderShippingMethod;
