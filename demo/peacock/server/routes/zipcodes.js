const makeRouter = require('koa-router');
const zipcodes = require('zipcodes');

const router = makeRouter()
  .get('/node/lookup-zip/usa/:zipcode', function* () {
    const info = zipcodes.lookup(this.params.zipcode);
    if (!info) {
      this.status = 404;
    } else {
      this.body = Object.assign({}, info, {
        state: zipcodes.states.abbr[info.state],
      });
    }
  });

module.exports = router;
