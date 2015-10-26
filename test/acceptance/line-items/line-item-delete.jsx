'use strict';

const React = require('react');
const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');

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
    ReactDOM.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function *() {
    let lineItemDelete = ReactDOM.render(<DeleteLineItem onDelete={onDelete}/>, container);
    let renderedDOM = ReactDOM.findDOMNode(lineItemDelete);
    let lineItemDeleteNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithTag(lineItemDelete, 'button'));

    expect(lineItemDeleteNode).to.be.instanceof(Object);
  });

  it('should show modal when click on button', function *() {
    let lineItemDelete = ReactDOM.render(<DeleteLineItem onDelete={onDelete}/>, container);
    let modal = ReactDOM.render(<Modal />, modalContainer);
    let renderedDOM = ReactDOM.findDOMNode(lineItemDelete);
    let lineItemDeleteNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithTag(lineItemDelete, 'button'));

    TestUtils.Simulate.click(lineItemDeleteNode);
  });
});
