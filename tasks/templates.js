
const fs = require('fs');
const dot = require('dot');
const through = require('through2');

/* eslint no-param-reassign:0 */

module.exports = function(gulp) {
  const src = 'src/templates/main.html';

  gulp.task('templates', ['sprites'], function() {
    const evilIcons = require('evil-icons');

    const GA_TRACKING_ID = process.env.GA_TRACKING_ID;

    if (!GA_TRACKING_ID) {
      console.warn('WARNING. There is no google analytics tracking id configured.' +
        'Use GA_TRACKING_ID env variable for that.');
    }

    gulp.src(src)
      .pipe(through.obj((file, enc, cb) => {
        const fn = dot.template(
          file.contents.toString(),
          Object.assign({selfcontained: true}, dot.templateSettings),
          {
            evilSprite: evilIcons.sprite,
            fcSprite: String(fs.readFileSync('build/svg/fc-sprite.svg')),
            // use GA_LOCAL=1 gulp dev command for enable tracking events in google analytics from localhost
            gaEnableLocal: 'GA_LOCAL' in process.env,
            gaTrackingId: GA_TRACKING_ID,
          }
        ).toString();
        file.contents = new Buffer(`module.exports = ${fn}`);
        file.path += '.js';
        cb(null, file);
      }))
      .pipe(gulp.dest('build'));
  });

  gulp.task('templates.watch', function() {
    gulp.watch([src], ['templates']);
  });
};
