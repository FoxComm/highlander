
const _ = require('lodash');
const path = require('path');
const cp = require('child_process');

process.env.PRINT_ROUTES = 1;
process.env.NODE_PATH = _.compact([process.env.NODE_PATH, path.resolve('./lib')]).join(':');

cp.fork(path.resolve('./lib/boot_print.js'));
