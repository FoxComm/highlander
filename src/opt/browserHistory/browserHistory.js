'use strict';

let history = null;

exports.setHistory = function(h) {
  history = h;
};

exports.transitionTo = function(name, params) {
  return history.push({name, params});
};
