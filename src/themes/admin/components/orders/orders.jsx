'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

class Orders extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      tableRows: this.generateOrders()
    };
  }

  generateOrders() {
    let
      idx       = 50,
      orders    = [],
      status    = ['Remorse Hold', 'Fulfillment Started', 'Shipped'],
      payments  = ['Full Capture', 'Partial Capture'],
      shipping  = [null, 'New'];

    while (idx--) {
      orders.push({
        order: 10000 + idx,
        date: new Date().toISOString(),
        email: `bob${idx}@foxcommerce.com`,
        orderStatus: status[~~(Math.random() * status.length)],
        paymentStatus: payments[~~(Math.random() * payments.length)],
        shippingStatus: shipping[~~(Math.random() * shipping.length)],
        total: '$100.00'
      });
    }
    return orders;
  }

  render() {
    return (
      <div id="orders">
        <div className="gutter">
          <table className='listing'>
            <TableHead columns={this.props.tableColumns}/>
            <TableBody columns={this.props.tableColumns} rows={this.state.tableRows}/>
          </table>
        </div>
      </div>
    );
  }
}

Orders.propTypes = {
  tableColumns: React.PropTypes.array
};

Orders.defaultProps = {
  tableColumns: [
    {field: 'order', text: 'Order'},
    {field: 'date', text: 'Date'},
    {field: 'email', text: 'Email'},
    {field: 'orderStatus', text: 'Order Status'},
    {field: 'paymentStatus', text: 'Payment Status'},
    {field: 'shippingStatus', text: 'Shipping Status'},
    {field: 'total', text: 'Total'}
  ]
};

export default Orders;
