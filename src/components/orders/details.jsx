'use strict';

import React from 'react';
import OrderSummary from './summary';
import CustomerInfo from './customer-info';
import LineItems from '../line-items/line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './shipping-method';
import OrderPayment from './payment';
import { Map } from 'immutable';


export default class OrderDetails extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      isInEditMode: false
    };
  }

  updateLineItems(data) {
  }

  get isInEditMode() {
    return this.state.isInEditMode;
  }

  toggleEdit() {
    const currentState = this.isInEditMode;
    this.setState({isInEditMode: !currentState});
  }

  render() {
    let order = this.props.order;
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

    if (this.isInEditMode) {
      actions = (
        <button className="fc-btn"
                onClick={this.toggleEdit.bind(this)}>
          Cancel Edits
        </button>
      );
    } else {
      actions = (
        <button className="fc-btn fc-btn-primary"
                onClick={this.toggleEdit.bind(this)}>
          Edit Order Details
        </button>
      );
    }

    return (
      <div className="fc-order-details">
        <div className="fc-order-details-controls">
          { actions }
        </div>
        <div className="fc-order-details-body">
          <div className="fc-order-details-main">
            <LineItems
              entity={order}
              tableColumns={lineColumns}
              model={'order'}
              onChange={this.updateLineItems.bind(this)}
              editMode={ this.isInEditMode }/>
            <OrderShippingAddress order={order} editMode={ this.isInEditMode }/>
            <OrderShippingMethod order={order} editMode={ this.isInEditMode }/>
            <OrderPayment order={order} editMode={ this.isInEditMode }/>
          </div>
          <div className="fc-order-details-aside">
            <OrderSummary order={order} />
            <CustomerInfo order={order} />
          </div>
        </div>
      </div>
    );
  }
}

OrderDetails.propTypes = {
  order: React.PropTypes.object
};
