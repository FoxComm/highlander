'use strict';

const _ = require('lodash');
const fs = require('fs');
const path = require('path');
const gulp = require('gulp');
const runSequence = require('run-sequence');
const $ = require('gulp-load-plugins')();
const Config = require('./config');

process.env.NODE_PATH = `${process.env.NODE_PATH}:${path.resolve('./src')}`;

const opts = new Config().gulp;

for (let task of fs.readdirSync(opts.taskDir)) {
  let file = path.join(opts.taskDir, task);
  require(file)(gulp, opts, $);
}

function handleErrors(err) {
  if (err) {
    console.error(err);
  }
  process.exitCode = 1;
}

process.on('unhandledRejection', handleErrors);
process.on('uncaughtException', handleErrors);

gulp.on('err', handleErrors);
