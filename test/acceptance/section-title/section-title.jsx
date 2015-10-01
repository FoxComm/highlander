'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');

describe('SectionTitle', function() {
  let SectionTitle = require(path.resolve('src/components/section-title/section-title.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should render', function *() {
    let sectionTitle = React.render(
      <SectionTitle title='Orders'/>
      , container);
    let sectionTitleNode = TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid').getDOMNode();

    expect(sectionTitleNode).to.be.instanceof(Object);
    expect(sectionTitleNode.className).to.contain('fc-grid');
    expect(sectionTitleNode.className).to.contain('gutter');
  });

  it('should not render counter if count is not set', function *() {
    let sectionTitle = React.render(
      <SectionTitle title='Orders'/>
      , container);
    let sectionTitleNode = TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid').getDOMNode();

    expect(sectionTitleNode.querySelector('.fc-subtitle')).to.be.equal(null);
  });

  it('should render counter if count is set', function *() {
    let count = 100;
    let sectionTitle = React.render(
      <SectionTitle title='Orders' count={ count } />
      , container);
    let sectionTitleNode = TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid').getDOMNode();

    expect(sectionTitleNode.querySelector('.fc-subtitle').innerHTML).to.be.equal('' + count);
  });

  it('should not render button if handler is not set', function *() {
    let sectionTitle = React.render(
      <SectionTitle title='Orders'/>
      , container);
    let sectionTitleNode = TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid').getDOMNode();

    expect(sectionTitleNode.querySelector('button.fc-btn.fc-btn-primary')).to.be.equal(null);
  });

  it('should render button if handler is set', function *() {
    let title = 'Orders';
    let handler = function(){};
    let sectionTitle = React.render(
      <SectionTitle title={ title } buttonClickHandler={ handler }/>
      , container);
    let sectionTitleNode = TestUtils.findRenderedDOMComponentWithClass(sectionTitle, 'fc-grid').getDOMNode();

    expect(sectionTitleNode.querySelector('button.fc-btn.fc-btn-primary').innerHTML).to.contain(title);
  });
});
