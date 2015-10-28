'use strict';

require('testdom')('<html><body></body></html>');

const path = require('path');
const ReactDOM = require('react-dom');
const TestUtils = require('react-addons-test-utils');
const ShallowTestUtils = require('react-shallow-testutils');

const unexpectedReactShallow = require('unexpected-react-shallow');

global.unexpected = global.unexpected.clone()
  .installPlugin(unexpectedReactShallow);


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

global.createContainer = function(tagName = 'div', attachToDom = false) {
  const container = document.createElement(tagName);
  if (attachToDom) {
    document.body.appendChild(container);
  }

  container.unmount = function() {
    if (attachToDom) {
      document.body.removeChild(container);
    }
    ReactDOM.unmountComponentAtNode(container);
  };

  return container;
};

global.renderIntoDocument = function(element, attachToDom = false) {
  return new Promise((resolve, reject) => {
    const container = global.createContainer(void 0, attachToDom);
    const instance = ReactDOM.render(element, container, later(function() {
      instance.unmount = container.unmount;
      instance.container = container;
      resolve(instance);
    }));
  });
};
