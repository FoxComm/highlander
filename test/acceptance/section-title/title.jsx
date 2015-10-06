'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react');

const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');

const path = require('path');

describe('Title', function() {
  let Title = require(path.resolve('src/components/section-title/title.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    ReactDOM.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should contain title text', function *() {
    let titleText = 'Orders';
    let subtitleText = '40237';

    let title = ReactDOM.render(
      <Title title={ titleText } subtitle={ subtitleText }/>
      , container);
    let titleNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(title, 'fc-title'));

    expect(titleNode.innerHTML).to.contain(titleText);
  });

  it('should contain subtitle text in span', function *() {
    let titleText = 'Orders';
    let subtitleText = '40256';

    let title = ReactDOM.render(
      <Title title={ titleText } subtitle={ subtitleText }/>
      , container);
    let titleNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(title, 'fc-title'));

    expect(titleNode.querySelector('.fc-subtitle').innerHTML).to.contain(subtitleText);
  });

  it('should contain title text only', function *() {
    let titleText = 'Orders';
    let title = ReactDOM.render(
      <Title title={ titleText }/>
      , container);
    let titleNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(title, 'fc-title'));

    expect(titleNode.innerHTML).to.be.equal(titleText);
  });
});
