'use strict';

require('testdom')('<html><body></body></html>');

const
  React = require('react/addons'),
  TestUtils = React.addons.TestUtils,
  path = require('path');

describe('Orders List', function() {
  let
    Orders = null,
    orders = null;

  before(function() {
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(document.body);
    setTimeout(done);
  });

  it('should have a list of orders', function *() {
  });
});
