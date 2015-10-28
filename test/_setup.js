'use strict';

const unexpected = require('unexpected');
global.unexpected = unexpected;

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
