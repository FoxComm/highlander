const router = require('koa-router')();

const suggestFixture = require('./suggest.fixture');
const fieldsFixture = require('./fields.fixture');

function processList(list) {
  if (!list) {
    return null;
  }

  return list.map(item => {
    let categories = item.node_path.split('/');
    const deepest = categories.pop();

    return {
      id: item.node_id,
      item_type: item.item_type,
      department: item.department,
      text: deepest,
      prefix: categories.join(' » '),
    };
  });
}

function delay(ms) {
  return function(callback) {
    setTimeout(callback, ms);
  };
}

router.get('/api/v1/amazon/categories/suggester', function *(next) {
  const primary = processList([suggestFixture.primary]);
  const secondary = processList(suggestFixture.secondary);
  const send = {
    primary,
    secondary,
  };

  this.body = send;
});

router.get('/api/v1/amazon/categories/schema', function *(next) {
  this.body = fieldsFixture;
});

router.get('/api/v1/amazon/credentials/:customer_id', function *(next) {
  const customer_id = this.params.customer_id;

  // @todo request to real hyperion

  this.body = {customer_id, seller_id: '', mws_auth_token: ''};
});

router.post('/api/v1/amazon/credentials', function *(next) {
  const { seller_id, mws_auth_token } = this.request.body;

  if (!seller_id || !mws_auth_token) {
    this.status = 400;
    this.body = 'All fields are required';
    return;
  }

  // @todo request to real hyperion

  yield delay(4000);

  this.body = {status: 'ok'};

  // @todo handle Amazon auth fail (wrong credentials)
  // this.body = {status: 'not_ok'};
});

module.exports = router;
