
const crypto = require('crypto');
const path = require('path');

function generateLongName(exportedName, filepath) {
  const sanitisedPath = path.relative(process.cwd(), filepath)
    .replace('src/components', '')
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
  require('postcss-modules-values'),
  require('postcss-modules-extract-imports'),
  require('postcss-modules-local-by-default'),
  require('postcss-modules-scope')({
    generateScopedName,
  }),
  require('postcss-cssnext'),
];

exports.installHook = function() {
  require('css-modules-require-hook')({
    use: plugins,
  });
};

exports.plugins = plugins;
