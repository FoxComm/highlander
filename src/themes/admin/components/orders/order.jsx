'use strict';

import React from 'react';

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
          <time datetime={order.date}>{order.date}</time>
        </div>
      </div>
    );
  }
}

Order.propTypes = {
  customer: React.PropTypes.object,
  order: React.PropTypes.object
};

export default Order;
