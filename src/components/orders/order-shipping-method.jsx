import React, { PropTypes } from 'react';
import PanelHeader from './panel-header';
import ShippingMethod from '../shipping/shipping-method';

const OrderShippingMethod = props => {
  const shippingMethod = props.order.currentOrder.shippingMethod;
  const title = <PanelHeader isCart={props.isCart} status={props.status} text="Shipping Method" />;

  return (
    <ShippingMethod
      currentOrder={props.order.currentOrder}
      title={title}
      readOnly={props.readOnly}
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
  isCart: PropTypes.bool,
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
  status: PropTypes.string,
  updateShippingMethod: PropTypes.func,
  readOnly: PropTypes.bool,
};

OrderShippingMethod.defaultProps = {
  isCart: false,
  status: 'success',
  readOnly: false,
};


export default OrderShippingMethod;
