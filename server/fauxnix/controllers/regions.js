'use strict';

module.exports = function (app, router) {
  const Region = app.seeds.models.Region;

  router
    .get('/regions', function *() {
      this.body = Region.paginate(50, 1);
    });
};
