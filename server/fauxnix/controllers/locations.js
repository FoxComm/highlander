'use strict';

module.exports = function (app, router) {
  const State = app.seeds.models.State;

  router
    .get('/states', function *() {
      this.body = State.paginate(50, 1);
    });
};
