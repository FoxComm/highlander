'use strict';

const
  fs    = require('fs'),
  path  = require('path');

module.exports = function(env) {
  return {
    taskDir: path.resolve('tasks'),
    themeDir: path.resolve('src', 'themes'),
    publicDir: path.resolve('public'),
    testDir: path.resolve('test'),
    configSrc: path.resolve('config', '*.js'),
    serverSrc: path.resolve('server', '**/*.{js,json}'),
    jsSrc: path.resolve('src', 'themes', '**/*.{js,jsx}'),
    lessSrc: path.resolve('src', 'themes', '**/*.less'),
    imageSrc: path.resolve('src', 'themes', '**/*.{png,jpg,gif}'),
    getThemes: function(dir) {
      return fs.readdirSync(dir)
        .filter(function(file) {
          return fs.statSync(path.join(dir, file)).isDirectory();
      });
    }
  }
}
