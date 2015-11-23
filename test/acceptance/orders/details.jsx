
import React from 'react';

describe('OrderDetails', function() {
  const OrderDetails = requireComponent('orders/details.jsx');
  const order = require('../../fixtures/order.json');

  it('should render', function() {
    const props = {
      order: {
        currentOrder: order
      }
    };
    const orderDetails = OrderDetails(props);

    expect(orderDetails).to.be.instanceof(Object);
    expect(orderDetails.props.className).to.contain('fc-order-details');
  });
});
