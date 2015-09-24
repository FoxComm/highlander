'use strict';

import React from 'react';
import OrderSummary from './summary';
import CustomerInfo from './customer-info';
import LineItems from '../line-items/line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './shipping-method';
import OrderPayment from './payment';
import OrderStore from './../../stores/orders';
import { dispatch } from '../../lib/dispatcher';

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

  render() {
    let order = this.props.order;
    let isEditing = this.state.isEditing;
    let actions = null;
    let lineColumns = [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'skuId', text: 'SKU'},
      {field: 'price', text: 'Price', type: 'currency'},
      {field: 'qty', text: 'Quantity'},
      {field: 'total', text: 'Total', type: 'currency'},
      {field: 'status', text: 'Shipping Status'}
    ];

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
            <LineItems
              entity={order}
              isEditing={isEditing}
              tableColumns={lineColumns}
              model={'order'}
              />
            <OrderShippingAddress order={order}/>
            <OrderShippingMethod order={order} isEditing={isEditing}/>
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
