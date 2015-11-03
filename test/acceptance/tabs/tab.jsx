'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react');

const TestUtils = require('react-addons-test-utils');
const ReactDOM = require('react-dom');

const path = require('path');

describe('TabView', function() {
  let TabView = require(path.resolve('src/components/tabs/tab.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    ReactDOM.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should contain title text', function *() {
    let titleText = 'All';

    let tab = ReactDOM.render(
      <TabView>{ titleText }</TabView>
      , container);
    let tabNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(tab, 'fc-tab'));

    expect(tabNode.innerHTML).to.contain(titleText);
  });

  it('should be draggable by default', function *() {
    let titleText = 'All';

    let tab = ReactDOM.render(
      <TabView>{ titleText }</TabView>
      , container);
    let tabNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(tab, 'fc-tab'));

    expect(tabNode.querySelector('.icon-drag-drop')).to.be.instanceOf(Object);
  });

  it('should be draggable when property is false', function *() {
    let titleText = 'All';

    let tab = ReactDOM.render(
      <TabView draggable={ false }>{ titleText }</TabView>
      , container);
    let tabNode = ReactDOM.findDOMNode(TestUtils.findRenderedDOMComponentWithClass(tab, 'fc-tab'));

    expect(tabNode.querySelector('.icon-drag-drop')).to.be.equal(null);
  });
});
