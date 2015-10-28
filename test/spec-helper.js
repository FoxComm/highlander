'use strict';

const unexpected = require('unexpected');
const unexpectedReactShallow = require('unexpected-react-shallow');

const unexpectedExpect = unexpected.clone()
  .installPlugin(unexpectedReactShallow);

global.expect = (function(expect) {
  return function(target) {
    if (arguments.length > 1) {
      return unexpectedExpect.apply(this, arguments);
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
