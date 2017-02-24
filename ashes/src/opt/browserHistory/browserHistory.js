'use strict';

var history = null;

function setHistory(h) {
  history = h;
};

function transitionTo(name, params) {
  return history.push({
    name: name,
    params: params
  });
};

function transitionToLazy(name, params = {}) {
  return function () {
    return transitionTo(name, params);
  };
};

exports.setHistory = setHistory;
exports.transitionTo = transitionTo;
exports.transitionToLazy = transitionToLazy;
