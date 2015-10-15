'use strict';

global.expect = require('chai').expect;

global.later = function(func) {
  return function() {
    var args = arguments;
    process.nextTick(function() {
      func.apply(this, args);
    });
  };
};
