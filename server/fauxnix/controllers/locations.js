'use strict';

module.exports = function (app, router) {
  const Country = app.seeds.models.Country;
  const Region = app.seeds.models.Region;

  router
    .get('/countries', function *() {
      this.body = Country.paginate(50, 1);
    })
    .get('/countries/:id', function *() {
      this.body = Region.paginate(50, 1);
    });
};
