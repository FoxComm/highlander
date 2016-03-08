import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

import * as shippingMethodActions from '../../modules/orders/shipping-methods';

import PanelHeader from './panel-header';
import ShippingMethod from '../shipping/shipping-method';

const mapStateToProps = state => {
  return { shippingMethods: state.orders.shippingMethods };
};

export class OrderShippingMethod extends React.Component {
  static propTypes = {
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

  static defaultProps = {
    isCart: false,
    status: 'success',
    readOnly: false,
  };

  render() {
    const props = this.props;

    const shippingMethod = props.order.currentOrder.shippingMethod;
    const title = <PanelHeader isCart={props.isCart} status={props.status} text="Shipping Method" />;

    const isCheckingOut = _.get(props, 'order.isCheckingOut', false);
    const editAction = isCheckingOut
      ? null
      : () => props.fetchShippingMethods(props.order.currentOrder);

    return (
      <ShippingMethod
        currentOrder={props.order.currentOrder}
        title={title}
        readOnly={props.readOnly || !props.isCart}
        availableShippingMethods={props.shippingMethods.availableMethods}
        shippingMethods={[shippingMethod]}
        isEditing={props.shippingMethods.isEditing}
        editAction={editAction}
        doneAction={props.orderShippingMethodCancelEdit}
        updateAction={props.updateShippingMethod}
        isEditingPrice={props.shippingMethods.isEditingPrice}
        editPriceAction={props.orderShippingMethodStartEditPrice}
        cancelPriceAction={props.orderShippingMethodCancelEditPrice} />
    );
  }
}

export default connect(mapStateToProps, shippingMethodActions)(OrderShippingMethod);
