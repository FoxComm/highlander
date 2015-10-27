
import React from 'react';
import TestUtils from 'react-addons-test-utils';
const order = require('../orders/order-sample.json');

describe('LineItemDelete', function() {
  const DeleteLineItem = requireComponent('line-items/line-item-delete.jsx');
  const onDelete = () => {};

  it('should render', function *() {
    let lineItemDelete = shallowRender(<DeleteLineItem onDelete={onDelete}/>);

    expect(lineItemDelete.type).to.equal('button');

    lineItemDelete.unmount();
  });
});
