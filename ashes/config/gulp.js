'use strict';

const
  fs    = require('fs'),
  path  = require('path');

module.exports = function() {
  return {
    taskDir: path.resolve('tasks'),
    srcDir: path.resolve('src'),
    publicDir: path.resolve('public'),
    assetsDir: path.resolve('public', 'admin'),
    testDir: path.resolve('test'),
    testSrc: path.resolve('test', '**/*.js'),
    configSrc: path.resolve('config', '*.js'),
    serverSrc: path.resolve('server', '**/*.{js,json}'),
    jsSrc: path.resolve('src', '**/*.{js,jsx}'),
    lessSrc: path.resolve('src', '**/*.less'),
    imageSrc: path.resolve('src', '**/*.{png,jpg,gif}'),
    getThemes: function(dir) {
      return fs.readdirSync(dir)
        .filter(function(file) {
          return fs.statSync(path.join(dir, file)).isDirectory();
        });
    }
  };
};
