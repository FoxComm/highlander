'use strict';

require('testdom')('<html><body></body></html>');

const React = require('react/addons');
const TestUtils = React.addons.TestUtils;
const path = require('path');

describe('TabView', function() {
  let TabView = require(path.resolve('src/components/tabs/tab.jsx'));
  let container = null;

  beforeEach(function() {
    container = document.createElement('div');
  });

  afterEach(function(done) {
    React.unmountComponentAtNode(container);
    setTimeout(done);
  });

  it('should contain title text', function *() {
    let titleText = 'All';

    let tab = React.render(
      <TabView>{ titleText }</TabView>
      , container);
    let tabNode = TestUtils.findRenderedDOMComponentWithClass(tab, 'fc-tab').getDOMNode();

    expect(tabNode.innerHTML).to.contain(titleText);
  });

  it('should be draggable by default', function *() {
    let titleText = 'All';

    let tab = React.render(
      <TabView>{ titleText }</TabView>
      , container);
    let tabNode = TestUtils.findRenderedDOMComponentWithClass(tab, 'fc-tab').getDOMNode();

    expect(tabNode.querySelector('.icon-drag-drop')).to.be.instanceOf(Object);
  });

  it('should be draggable when property is false', function *() {
    let titleText = 'All';

    let tab = React.render(
      <TabView draggable={ false }>{ titleText }</TabView>
      , container);
    let tabNode = TestUtils.findRenderedDOMComponentWithClass(tab, 'fc-tab').getDOMNode();

    expect(tabNode.querySelector('.icon-drag-drop')).to.be.equal(null);
  });
});
