'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react');

const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');

const path = require('path');

describe('SectionTitle', function() {
  let SectionTitle = require(path.resolve('src/components/section-title/section-title.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    ReactDOM.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function *() {
    let sectionTitle = ReactDOM.render(
      <SectionTitle title='Orders'/>
      , container);
    let sectionTitleNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid'));

    expect(sectionTitleNode).to.be.instanceof(Object);
    expect(sectionTitleNode.className).to.contain('fc-grid');
    expect(sectionTitleNode.className).to.contain('gutter');
  });

  it('should not render counter if count is not set', function *() {
    let sectionTitle = ReactDOM.render(
      <SectionTitle title='Orders'/>
      , container);
    let sectionTitleNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid'));

    expect(sectionTitleNode.querySelector('.fc-subtitle')).to.be.equal(null);
  });

  it('should render counter if count is set', function *() {
    let count = 100;
    let sectionTitle = ReactDOM.render(
      <SectionTitle title='Orders' count={ count } />
      , container);
    let sectionTitleNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid'));

    expect(sectionTitleNode.querySelector('.fc-subtitle').innerHTML).to.be.equal('' + count);
  });

  it('should not render button if handler is not set', function *() {
    let sectionTitle = ReactDOM.render(
      <SectionTitle title='Orders'/>
      , container);
    let sectionTitleNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid'));

    expect(sectionTitleNode.querySelector('button.fc-btn.fc-btn-primary')).to.be.equal(null);
  });

  it('should render button if handler is set', function *() {
    let title = 'Orders';
    let handler = function(){};
    let sectionTitle = ReactDOM.render(
      <SectionTitle title={ title } buttonClickHandler={ handler }/>
      , container);
    let sectionTitleNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid'));

    expect(sectionTitleNode.querySelector('button.fc-btn.fc-btn-primary').innerHTML).to.contain(title);
  });
});
