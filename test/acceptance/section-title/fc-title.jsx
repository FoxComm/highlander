'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');

describe('FCTitle', function() {
  let FCTitle = require(path.resolve('src/components/section-title/fc-title.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should contain title text', function *() {
    let titleText = 'Orders';
    let title = React.render(
      <FCTitle title={ titleText }/>
      , container);
    let titleNode = TestUtils.findRenderedDOMComponentWithClass(title, 'fc-title').getDOMNode();

    expect(titleNode.innerHTML).to.be.equal(titleText);
  });
});
