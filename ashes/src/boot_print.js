
require('babel-polyfill');
require('./postcss').installHook();

require('./routes-print').print();
