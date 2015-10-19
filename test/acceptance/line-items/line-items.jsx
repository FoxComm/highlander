'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react');
const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');
const path = require('path');
const order = require('../orders/order-sample.json');

describe('OrderLineItems', function() {
  let LineItems = require(path.resolve('src/components/line-items/line-items.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    ReactDOM.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function *() {
    let orderLineItems = ReactDOM.render(
      <LineItems entity={order} model='order'/>
      , container);
    let orderLineItemsNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(orderLineItems, 'fc-line-items'));

    expect(orderLineItemsNode).to.be.instanceof(Object);
    expect(orderLineItemsNode.className).to.contain('fc-line-items');
  });

  it('should switch to edit mode when click on Edit line items button', function *() {
    let orderLineItems = ReactDOM.render(
      <LineItems entity={order} model='order'/>
      , container);
    let renderedDOM = () => ReactDOM.findDOMNode(orderLineItems);
    let editButtons = renderedDOM().querySelectorAll('.fc-content-box-header .fc-btn');
    let doneButtons = renderedDOM().querySelectorAll('footer .fc-btn');

    expect(editButtons).to.have.length(1);
    expect(doneButtons).to.have.length(0);
    TestUtils.Simulate.click(editButtons[0]);

    let editButtons2 = renderedDOM().querySelectorAll('.fc-content-box-header .fc-btn');
    let doneButtons2 = renderedDOM().querySelectorAll('footer .fc-btn');
    expect(editButtons2).to.have.length(0);
    expect(doneButtons2).to.have.length(1);
  });

});
