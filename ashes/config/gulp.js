'use strict';

const path = require('path');

module.exports = function() {
  return {
    taskDir: path.resolve('tasks'),
    srcDir: path.resolve('src'),
    testDir: path.resolve('test'),
    testSrc: path.resolve('test', '**/*.js'),
    configSrc: path.resolve('config', '*.js'),
    serverSrc: path.resolve('server', '**/*.{js,json}'),
    jsSrc: path.resolve('src', '**/*.{js,jsx}'),
  };
};
