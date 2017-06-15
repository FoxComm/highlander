const crypto = require('crypto');
const path = require('path');

// '../../src/components/product/page' + 'wrapper' -> 'product/page__wrapper'
function generateLongName(exportedName, filepath) {
  const sanitisedPath = path.relative(process.cwd(), filepath)
    .replace('src/components', '')
    .replace('lib/components', '')
    .replace('src/css', '')
    .replace(/\.[^\.\/\\]+$/, '')
    .replace(/^[\.\/\\]+/, '')
    .replace(/\//g, '⁄'); // http://www.fileformat.info/info/unicode/char/2044/browsertest.htm

  const sanitisedName = exportedName.replace(/^_+|_+$/g, '');

  return `${sanitisedPath}__${sanitisedName}`;
}

function generateShortName(name, filename, css) {
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
  require('postcss-url')({ url: 'inline', maxSize: 4 }),
  // @todo remove second `postcss-url` after https://github.com/postcss/postcss-url/issues/104
  require('postcss-url')({ url: 'rebase', to: './src' }),
  require('postcss-cssnext')({
    features: {
      // Instead of it we are using `postcss-css-variables` below
      // https://github.com/MadLittleMods/postcss-css-variables#differences-from-postcss-custom-properties
      customProperties: false,
    },
  }),
  require('postcss-mixins'),
  require('postcss-nested'),
  require('postcss-css-variables'),
  require('postcss-modules-local-by-default'),
  require('postcss-modules-scope')({
    generateScopedName,
  }),
];

// uncomment and then
// ./node_modules/.bin/postcss-debug -c ./postcss.config.js ./styleguide/styleguide.css
// module.exports = function(postcss) {
//   return postcss(plugins);
// };

module.exports.plugins = plugins;
