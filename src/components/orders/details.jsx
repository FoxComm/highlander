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
import Api from '../../lib/api';
import LineItemStore from '../../stores/line-items';

export default class OrderDetails extends React.Component {
  updateLineItems(data) {
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

    return (
      <div className="fc-order-details">
        <div className="fc-order-details-body">
          <div className="fc-order-details-main">
            <LineItems
              entity={order}
              tableColumns={lineColumns}
              model={'order'}
              onChange={this.updateLineItems.bind(this)} />
            <OrderShippingAddress order={order} />
            <OrderShippingMethod order={order} />
            <OrderPayment order={order} />
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
