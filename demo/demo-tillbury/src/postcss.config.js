const crypto = require('crypto');
const path = require('path');

function generateLongName(exportedName, filepath) {
  const sanitisedPath = path.relative(process.cwd(), filepath)
    .replace('src/components', '')
    .replace('lib/components', '')
    .replace(/\.[^\.\/\\]+$/, '')
    .replace(/[\W_]+/g, '_')
    .replace(/^_|_$/g, '');
  return `_${sanitisedPath}__${exportedName}`;
}

function generateShortName (name, filename, css) {
  const i = css.indexOf(`.${name}`);
  const numLines = css.substr(0, i).split(/[\r\n]/).length;

  const hash = crypto.createHash('md5').update(css).digest('hex').substr(0, 5);
  return `_${name}_${hash}_${numLines}`;
}

const generateScopedName = process.env.NODE_ENV === 'production' ? generateShortName : generateLongName;

const plugins = [
  require('postcss-import')({
    path: ['src/css', 'node_modules'],
  }),
  require('postcss-css-variables'),
  require('postcss-apply'),
  require('lost')({
    flexbox: 'flex',
    gutter: '2.4%',
  }),
  require('postcss-modules-values'),
  require('postcss-modules-extract-imports'),
  require('postcss-modules-local-by-default'),
  require('postcss-modules-scope')({
    generateScopedName,
  }),
  require('postcss-cssnext')({
    features: {
      customProperties: false,
      appyRule: false,
    },
  }),
];

exports.installHook = function() {
  const map = require('../build/css-modules.json');

  require.extensions['.css'] = function(m, filename) {
    const relativePath = path.relative(process.cwd(), filename);

    const tokens = map[relativePath];
    return m._compile(`module.exports = ${JSON.stringify(tokens)}`, filename);
  };
};

exports.plugins = plugins;
