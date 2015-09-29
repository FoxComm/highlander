'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');
const order = require('../orders/order-sample.json');

describe('LineItemDelete', function() {
  let DeleteLineItem = require(path.resolve('src/components/line-items/line-item-delete.jsx'));
  let Modal = require(path.resolve('src/components/modal/modal.jsx'));
  let container = null;
  let modalContainer = null;
  let onDelete = () => {return;};

  beforeEach(function() {
    container = document.createElement('div');
    modalContainer = document.createElement('div');
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function *() {
    let lineItemDelete = React.render(<DeleteLineItem onDelete={onDelete}/>, container);
    let renderedDOM = React.findDOMNode(lineItemDelete);
    let lineItemDeleteNode = TestUtils.findRenderedDOMComponentWithTag(lineItemDelete, 'button').getDOMNode();

    expect(lineItemDeleteNode).to.be.instanceof(Object);
  });

  it('should show modal when click on button', function *() {
    let lineItemDelete = React.render(<DeleteLineItem onDelete={onDelete}/>, container);
    let modal = React.render(<Modal />, modalContainer);
    let renderedDOM = React.findDOMNode(lineItemDelete);
    let lineItemDeleteNode = TestUtils.findRenderedDOMComponentWithTag(lineItemDelete, 'button').getDOMNode();

    TestUtils.Simulate.click(lineItemDeleteNode);

    let modalNode = TestUtils.findRenderedDOMComponentWithClass(modal, 'show').getDOMNode();
    expect(modalNode).to.be.instanceof(Object);
  });
});
