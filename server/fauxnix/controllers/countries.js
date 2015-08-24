'use strict';

module.exports = function (app, router) {
  const Country = app.seeds.models.Country;

  router
    .get('/countries', function *() {
      this.body = Country.paginate(50, 1);
    });
};
