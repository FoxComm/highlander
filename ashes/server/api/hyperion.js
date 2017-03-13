const router = require('koa-router')();

const suggestFixture = require('./suggest.fixture');
const fieldsFixture = require('./fields.fixture');

// @todo move all logic to hyperion, after that remove this file
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

// @todo remove after full integration
// router.get('/api/v1/amazon/categories/suggester', function *(next) {
//   const primary = processList([suggestFixture.primary]);
//   const secondary = processList(suggestFixture.secondary);
//   const send = {
//     primary,
//     secondary,
//   };

//   this.body = send;
// });

// router.get('/api/v1/amazon/categories/schema', function *(next) {
//   this.body = fieldsFixture;
// });

module.exports = router;
