
const path = require('path');
const through = require('through2');

/* eslint no-param-reassign:0 */

const spriteHead = new Buffer(`<svg
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    id="fc-sprite" style="display:none">`);

const spriteTail = new Buffer(`</svg>`);

function svgoOpts(file) {
  const plugins = [
    {
      removeTitle: true,
    },
    {
      mergePaths: false,
    },
    {
      convertPathData: {
        leadingZero: false,
      },
    },
    {
      convertToSymbols: {
        type: 'perItem',
        fn(item) {
          if (item.isElem('svg')) {
            item.removeAttr('width');
            item.removeAttr('height');
            item.removeAttr('xmlns');
            item.renameElem('symbol');
            item.addAttr({
              name: 'id',
              local: 'id',
              prefix: '',
              value: `fc-${path.basename(file.path, '.svg').toLowerCase()}-icon`,
            });
          }
        },
      },
    },
  ];

  return {plugins};
}

module.exports = function(gulp, $) {
  const src = 'src/images/svg/**/*.svg';

  gulp.task('sprites', function() {
    return gulp.src(src)
      .pipe($.svgmin(svgoOpts))
      .pipe($.concat('fc-sprite.svg'))
      .pipe(through.obj((file, enc, cb) => {
        file.contents = Buffer.concat([spriteHead, file.contents, spriteTail]);
        cb(null, file);
      }))
      .pipe(gulp.dest('build/svg'));
  });

  gulp.task('sprites.watch', function() {
    gulp.watch([src], ['sprites']);
  });
};
