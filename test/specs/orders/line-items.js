'use strict';

import _ from 'lodash';

describe('OrderLineItems', function() {
  context('#POST', function() {
    it('should update the lineItem', function *() {
      let res = yield this.api.post('/orders/1/line-items', [{'sku': 'HELLO', 'quantity': 1}]);
      let order = res.response;

      expect (res.status).to.equal(202);
      let lineItem = _.find(order.lineItems, function (item) {
        return item.sku === 'HELLO';
      });
      expect(lineItem.quantity).to.equal(1);
    });
  });
});
