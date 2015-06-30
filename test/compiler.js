'use strict';

const
  fs          = require('fs'),
  ReactTools  = require('react-tools');

require.extensions['.jsx'] = function(module, filename) {
  let content = fs.readFileSync(filename, 'utf8');
  let compiled = ReactTools.transform(content, {harmony: true});
  return module._compile(compiled, filename);
};
