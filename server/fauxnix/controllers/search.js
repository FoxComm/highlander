'use strict';

const
  fleck = require('fleck');

module.exports = function(app, router) {
  router
    .param('model', function *(model, next) {
      let modelName = fleck.inflect(model, 'singularize', 'capitalize');
      this.searchModel = app.seeds.models[modelName];
      yield next;
    })
    .get('/search/:model', function *() {
      let
        query   = this.request.query,
        limit   = +query.size || 20,
        page    = query.from ? +query.from - 1 : 0,
        models  = this.searchModel.findAll(query);
      this.body = models.slice(limit * page, (limit * page) + limit);
    });
};
