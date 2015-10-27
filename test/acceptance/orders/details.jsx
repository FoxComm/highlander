
import React from 'react';
import TestUtils from 'react-addons-test-utils';
import ReactDOM from 'react-dom';

describe('OrderDetails', function() {
  const OrderDetails = requireComponent('orders/details.jsx');
  const order = require('./order-sample.json');

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
