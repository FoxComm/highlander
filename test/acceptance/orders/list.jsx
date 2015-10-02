'use strict';

require('testdom')('<html><body></body></html>');

const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');
const path = require('path');

describe('Orders List', function() {
  let
    Orders = null,
    orders = null;

  before(function() {
  });

  afterEach(function(done) {
    ReactDOM.unmountComponentAtNode(document.body);
    setTimeout(done);
  });

  it('should have a list of orders', function *() {
  });
});
