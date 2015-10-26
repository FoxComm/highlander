'use strict';

require('testdom')('<html><body></body></html>');

const path = require('path');
const ReactDOM = require('react-dom');
const TestUtils = require('react-addons-test-utils');
const ShallowTestUtils = require('react-shallow-testutils');
require('./expect-jsx');

global.requireComponent = function(componentPath) {
  return require(path.resolve('src/components/' + componentPath));
};

global.shallowRender = function(element) {
  const renderer = TestUtils.createRenderer();
  renderer.render(element);

  Object.defineProperty(renderer, 'instance', {
    get: function() {
      return ShallowTestUtils.getMountedInstance(this);
    }
  });

  ['type', 'props'].map(property => {
    Object.defineProperty(renderer, property, {
      get: function() {
        return this.getRenderOutput()[property];
      }
    });
  });

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
