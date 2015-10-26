'use strict';

require('testdom')('<html><body></body></html>');

const path = require('path');
const ReactDOM = require('react-dom');
const TestUtils = require('react-addons-test-utils');
require('./expect-jsx');

global.requireComponent = function(componentPath) {
  return require(path.resolve('src/components/' + componentPath));
};

global.shallowRender = function(element) {
  const renderer = TestUtils.createRenderer();
  renderer.render(element);
  const result = renderer.getRenderOutput();
  Object.assign(renderer, result);

  return renderer;
};

// container for acceptance tests
global.container = null;

// install global hooks

beforeEach(function() {
  global.container = document.createElement('div');
  document.body.appendChild(global.container);
});

afterEach(function(done) {
  document.body.removeChild(global.container);
  ReactDOM.unmountComponentAtNode(global.container);
  setTimeout(done);
});
