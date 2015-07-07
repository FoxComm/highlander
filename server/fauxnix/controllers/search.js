'use strict';

module.exports = function(app, router) {
  router
    .get('/search', function *() {
      let query = this.request.query;
      console.log(query);
      this.status = 200;
    });
};
