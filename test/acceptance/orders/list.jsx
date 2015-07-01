'use strict';

require('testdom')('<html><body></body></html>');

const
  React     = require('react/addons'),
  TestUtils = React.addons.TestUtils,
  path      = require('path');

describe('Orders List', function() {
  let
    Orders = null,
    orders = null;

  before(function () {
    Orders = require(path.resolve('src/themes/admin/components/orders/orders'));
    orders = TestUtils.renderIntoDocument(
      <Orders/>
    );
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(document.body);
    setTimeout(done);
  });

  it('should have a list of orders', function *() {
    let inputComponent = TestUtils.findRenderedDOMComponentWithTag(order, 'input');
    let input = inputComponent.getDOMNode();
    expect(input.getAttribute('type')).to.equal('checkbox');
  });
});
