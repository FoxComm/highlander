'use strict';

var history = null;

exports.setHistory = function(h) {
  history = h;
};

exports.transitionTo = function(name, params) {
  return history.push({
    name: name,
    params: params
  });
};
