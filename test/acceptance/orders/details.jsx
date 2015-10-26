'use strict';

const React = require('react');
const path = require('path');
const order = require('./order-sample.json');

const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');

global.XMLHttpRequest = require('xmlhttprequest').XMLHttpRequest;
global.localStorage = require('localStorage');

describe('OrderDetails', function() {
  let OrderDetails = require(path.resolve('src/components/orders/details.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    ReactDOM.unmountComponentAtNode(container);
    setTimeout(done);
  });


  // it('should render', function() {
  //   let orderDetails = ReactDOM.render(
  //     <OrderDetails order={order}/>
  //     , container);
  //   let orderDetailsNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(orderDetails, 'fc-order-details'));

  //   expect(orderDetailsNode).to.be.instanceof(Object);
  //   expect(orderDetailsNode.className).to.contain('fc-order-details');
  // });
});
