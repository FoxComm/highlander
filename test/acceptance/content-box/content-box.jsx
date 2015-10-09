'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react');

const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');

const path = require('path');

describe('ContentBox', function() {
  let ContentBox = require(path.resolve('src/components/content-box/content-box.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    ReactDOM.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render container with correct title', function *() {
    let title = 'Customer Info';
    let contentBox = ReactDOM.render(
      <ContentBox title={ title } className="" />
      , container);
    let contentBoxTitleNode = ReactDOM.findDOMNode(
      TestUtils.findRenderedDOMComponentWithClass(contentBox, 'fc-title'));

    expect(contentBoxTitleNode.innerHTML).to.be.equal(title);
  });

  it('should render container with correct class', function *() {
    let className = 'test-class';
    let contentBox = ReactDOM.render(
      <ContentBox title="" className={ className } />
      , container);
    let contentBoxNode = ReactDOM.findDOMNode(
      TestUtils.findRenderedDOMComponentWithClass(contentBox, 'fc-content-box'));

    expect(contentBoxNode.className).to.be.equal(`${className} fc-content-box`);
  });

  it('should render container with action block when provided', function *() {
    let actionBlock = 'Actions!';
    let contentBox = ReactDOM.render(
      <ContentBox title="" className="" actionBlock={ actionBlock }/>
      , container);
    let contentBoxNode = ReactDOM.findDOMNode(
      TestUtils.findRenderedDOMComponentWithClass(contentBox, 'fc-controls'));

    expect(contentBoxNode.innerHTML).to.be.equal(actionBlock);
  });

  it('should not render action block by default', function *() {
    let contentBox = ReactDOM.render(
      <ContentBox title="" className="" />
      , container);
    let contentBoxNode = ReactDOM.findDOMNode(
      TestUtils.findRenderedDOMComponentWithClass(contentBox, 'fc-controls'));

    expect(contentBoxNode.innerHTML).to.be.equal('');
  });


})
