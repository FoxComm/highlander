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

  this.body = {customer_id, seller_id: 'seller_id', mws_auth_token: 'mws_auth_token'};
});

router.post('/api/v1/amazon/credentials', function *(next) {
  this.body = {status: 'ok'};
});

module.exports = router;
