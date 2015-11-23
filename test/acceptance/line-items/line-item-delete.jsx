
import React from 'react';
const order = require('../../fixtures/order.json');

describe('LineItemDelete', function() {
  const DeleteLineItem = requireComponent('line-items/line-item-delete.jsx');
  const onDelete = () => {};

  it('should render', function *() {
    let lineItemDelete = shallowRender(<DeleteLineItem onDelete={onDelete}/>);

    expect(lineItemDelete.type).to.equal('button');

    lineItemDelete.unmount();
  });
});
