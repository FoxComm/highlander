import _ from 'lodash';
import path from 'path';
import rewire from 'rewire';

const unexpected = require('unexpected');
global.unexpected = unexpected
  .use(require('./_unexpected_actions'));

global.TimeShift = require('./_timeshift');

global.expect = (function(expect) {
  return function(target) {
    if (arguments.length > 1) {
      return global.unexpected.apply(this, arguments);
    } else {
      return expect(target);
    }
  };
})(require('chai').expect);


global.later = function(func) {
  return function() {
    var args = arguments;
    process.nextTick(function() {
      func.apply(this, args);
    });
  };
};

global.localStorage = require('localStorage');

const modulesCache = {};

global.importSource = function(sourcePath, actionsToExport = []) {
  const finalPath = path.resolve(`./src/${sourcePath}`);
  if (!(finalPath in modulesCache)) {
    const importedModule = rewire(finalPath);

    actionsToExport.forEach(action => {
      importedModule[action] = importedModule.__get__(action);
    });

    modulesCache[finalPath] = importedModule;
  }

  return modulesCache[finalPath];
};

global.requireSource = function(sourcePath) {
  const finalPath = path.resolve(`./src/${sourcePath}`);
  return require(finalPath);
};


global.phoenixUrl = 'https://api.foxcommerce/';
