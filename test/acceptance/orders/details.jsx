'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');
const order = require('./order-sample.json');

describe('OrderDetails', function() {
  let OrderDetails = require(path.resolve('src/components/orders/details.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function *() {
    let orderDetails = React.render(
      <OrderDetails order={order}/>
      , container);
    let orderDetailsNode = TestUtils.findRenderedDOMComponentWithClass(orderDetails, 'fc-order-details').getDOMNode();

    expect(orderDetailsNode).to.be.instanceof(Object);
    expect(orderDetailsNode.className).to.contain('fc-order-details');
  });
});
