'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import { Link } from 'react-router';

class Order extends React.Component {
  render() {
    let
      customer  = this.props.customer,
      order     = this.props.order;
    return (
      <div id="order">
        <div className="viewers"></div>
        <div className="gutter">
          <h1>Order {order.id}<em>for{customer.name}</em></h1>
          <time dateTime={order.date}>{order.date}</time>
        </div>
        <div className="gutter sub-nav">
          <ul>
            <li><a href="">Details</a></li>
            <li><a href="">Shipments</a></li>
            <li><a href="">Returns</a></li>
            <li><a href="">Emails</a></li>
            <li><Link to="notes" params={{order: order.id}}>Notes</Link></li>
            <li><a href="">Activity Trail</a></li>
          </ul>
          <RouteHandler/>
        </div>
      </div>
    );
  }
}

Order.propTypes = {
  customer: React.PropTypes.object,
  order: React.PropTypes.object
};

Order.defaultProps = {
  customer: {
    name: 'Bruce Wayne'
  },
  order: {
    id: 12345,
    date: new Date().toISOString()
  }
};

export default Order;
