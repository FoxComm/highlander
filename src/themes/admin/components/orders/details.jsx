'use strict';

import React from 'react';
import OrderSummary from './summary';
import CustomerInfo from './customer-info';
import OrderLineItems from './line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './shipping-method';
import OrderPayment from './payment';
import OrderStore from './store';
import { dispatch } from '../../lib/dispatcher';
import Api from '../../lib/api';

export default class OrderDetails extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isEditing: false
    };
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
    dispatch('toggleOrderEdit');
  }

  updateLineItems(data) {
    Api.post(`/orders/${this.props.order.id}/line-items`, data)
      .then((res) => {
        OrderStore.update(res);
      })
      .catch((err) => {
        console.error(err);
      });
  }

  render() {
    let
      order     = this.props.order,
      isEditing = this.state.isEditing,
      actions   = null;

    if (isEditing) {
      actions = (
        <span>
          <button onClick={this.toggleEdit.bind(this)}>Cancel</button>
          <button className='primary'>Save Edits</button>
        </span>
      );
    } else if (OrderStore.holdStatusList.indexOf(order.orderStatus) !== -1 || 1) {
      actions = (
        <button className="order-details-edit-order" onClick={this.toggleEdit.bind(this)}>Edit Order Details</button>
      );
    }

    return (
      <div className="order-details">
        <div className="order-details-controls">
          {actions}
        </div>

        <div className="order-details-body">
          <div className="order-details-main">
            <OrderLineItems order={order} isEditing={isEditing} onChange={this.updateLineItems.bind(this)} />
            <OrderShippingAddress order={order} isEditing={isEditing}/>
            <OrderShippingMethod order={order} isEditing={isEditing} />
            <OrderPayment order={order} isEditing={isEditing}/>
          </div>
          <div className="order-details-aside">
            <OrderSummary order={order} isEditing={isEditing}/>
            <CustomerInfo order={order} isEditing={isEditing}/>
          </div>
        </div>
      </div>
    );
  }
}

OrderDetails.propTypes = {
  order: React.PropTypes.object
};
