'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');
const order = require('../orders/order-sample.json');

describe('OrderLineItems', function() {
  let LineItems = require(path.resolve('src/themes/admin/components/line-items/line-items.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function *() {
    let orderLineItems = React.render(
      <LineItems entity={order} model='order'/>
      , container);
    let orderLineItemsNode = TestUtils.findRenderedDOMComponentWithClass(orderLineItems, 'line-items').getDOMNode();

    expect(orderLineItemsNode).to.be.instanceof(Object);
    expect(orderLineItemsNode.className).to.contain('line-items');
  });

  it('should switch to edit mode when click on Edit line items button', function *() {
    let orderLineItems = React.render(
      <LineItems entity={order} model='order'/>
      , container);
    let renderedDOM = () => React.findDOMNode(orderLineItems);
    let editButtons = renderedDOM().querySelectorAll('header .fc-btn');
    let doneButtons = renderedDOM().querySelectorAll('footer .fc-btn');

    expect(editButtons).to.have.length(1);
    expect(doneButtons).to.have.length(0);
    TestUtils.Simulate.click(editButtons[0]);

    let editButtons2 = renderedDOM().querySelectorAll('header .fc-btn');
    let doneButtons2 = renderedDOM().querySelectorAll('footer .fc-btn');
    expect(editButtons2).to.have.length(0);
    expect(doneButtons2).to.have.length(1);
  });
});
