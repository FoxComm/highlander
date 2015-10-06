'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');

describe('Title', function() {
  let Title = require(path.resolve('src/components/section-title/title.jsx'));
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
    let subtitleText = '40237';

    let title = React.render(
      <Title title={ titleText } subtitle={ subtitleText }/>
      , container);
    let titleNode = TestUtils.findRenderedDOMComponentWithClass(title, 'fc-title').getDOMNode();

    expect(titleNode.innerHTML).to.contain(titleText);
  });

  it('should contain subtitle text in span', function *() {
    let titleText = 'Orders';
    let subtitleText = '40256';

    let title = React.render(
      <Title title={ titleText } subtitle={ subtitleText }/>
      , container);
    let titleNode = TestUtils.findRenderedDOMComponentWithClass(title, 'fc-title').getDOMNode();

    expect(titleNode.querySelector('.fc-subtitle').innerHTML).to.contain(subtitleText);
  });

  it('should contain title text only', function *() {
    let titleText = 'Orders';
    let title = React.render(
      <Title title={ titleText }/>
      , container);
    let titleNode = TestUtils.findRenderedDOMComponentWithClass(title, 'fc-title').getDOMNode();

    expect(titleNode.innerHTML).to.be.equal(titleText);
  });
});
