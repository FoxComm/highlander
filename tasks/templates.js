
const dot = require('dot');
const through = require('through2');

/* eslint no-param-reassign:0 */

module.exports = function(gulp) {
  gulp.task('templates', function() {
    const evilIcons = require('evil-icons');

    gulp.src('src/templates/main.html')
      .pipe(through.obj((file, enc, cb) => {
        const fn = dot.template(
          file.contents.toString(),
          Object.assign({selfcontained: true}, dot.templateSettings),
          {
            evilSprite: evilIcons.sprite,
          }
        ).toString();
        file.contents = new Buffer(`module.exports = ${fn}`);
        file.path += '.js';
        cb(null, file);
      }))
      .pipe(gulp.dest('build'));
  });
};
