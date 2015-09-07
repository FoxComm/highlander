'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');
const order = require('./order-sample.json');

describe('OrderDetails', function() {
  let OrderDetails = require(path.resolve('src/themes/admin/components/orders/details.jsx'));
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
    let orderDetailsNode = TestUtils.findRenderedDOMComponentWithClass(orderDetails, 'order-details').getDOMNode();

    expect(orderDetailsNode).to.be.instanceof(Object);
    expect(orderDetailsNode.className).to.contain('order-details');
  });

  it('should switch to edit mode when click on Edit Order Detaisl button', function *() {
    let orderDetails = React.render(
      <OrderDetails order={order}/>
      , container);
    let editDetailsButton = TestUtils.findRenderedDOMComponentWithClass(orderDetails, 'order-details-edit-order').getDOMNode();

    TestUtils.Simulate.click(editDetailsButton);

    let editButtons = TestUtils.scryRenderedDOMComponentsWithClass(orderDetails, 'order-details-edit-order');
    expect(editButtons).to.have.length(0);

    let controls = TestUtils.findRenderedDOMComponentWithClass(orderDetails, 'order-details-controls').getDOMNode();
    expect(controls.innerHTML).to.contain('Save Edits');
    expect(controls.innerHTML).to.contain('Cancel');
  });
});
