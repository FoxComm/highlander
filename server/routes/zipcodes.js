import makeRouter from 'koa-router';
import zipcodes from 'zipcodes';

const router = makeRouter()
  .get('/node/lookup-zip/usa/:zipcode', function*() {
    const info = zipcodes.lookup(this.params.zipcode);
    if (!info) {
      this.status = 404;
    } else {
      this.body = {
        ...info,
        state: zipcodes.states.abbr[info.state],
      };
    }
  });

export default router;
