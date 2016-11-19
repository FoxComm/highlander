require('./_testdom');

const _ = require('lodash');
const path = require('path');
const ReactDOM = require('react-dom');
const ReactTestUtils = require('react-addons-test-utils');
const ShallowTestUtils = require('react-shallow-testutils');

const unexpectedReactShallow = require('unexpected-react-shallow');

global.unexpected = global.unexpected.clone()
  .installPlugin(unexpectedReactShallow);


global.requireComponent = function(componentPath, returnDefault = true) {
  const result = require(path.resolve('src/components/' + componentPath));
  return returnDefault ? result.default : result;
};

global.shallowRender = function(element) {
  const renderer = ReactTestUtils.createRenderer();
  renderer.render(element);

  Object.defineProperty(renderer, 'instance', {
    get: function() {
      return _.get(this, '_instance._instance');
    }
  });

  ['type', 'props', '$$typeof', 'key', 'ref', '_store', '_owner'].map(property => {
    Object.defineProperty(renderer, property, {
      get: function() {
        return this.getRenderOutput() && this.getRenderOutput()[property];
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

global.wait = function(ms) {
  return new Promise(resolve => {
    setTimeout(resolve, ms);
  });
};

