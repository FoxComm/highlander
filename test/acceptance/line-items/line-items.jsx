
import React from 'react';
import TestUtils from 'react-addons-test-utils';
const ReactDOM = require('react-dom');
const order = require('../orders/order-sample.json');

 describe('OrderLineItems', function() {
   const LineItems = requireComponent('line-items/line-items.jsx');
   let orderLineItems = null;

   afterEach(function() {
     if (orderLineItems) {
       orderLineItems.unmount();
       orderLineItems = null;
     }
   });

   it('should render', function *() {
     orderLineItems = yield renderIntoDocument(
       <LineItems entity={order} model='order'/>
     );

     const orderLineItemsNode = TestUtils.findRenderedDOMComponentWithClass(orderLineItems, 'fc-line-items');

     expect(orderLineItemsNode).to.be.instanceof(Object);
     expect(orderLineItemsNode.className).to.contain('fc-line-items');
   });

   it('should switch to edit mode when click on Edit line items button', function *() {
     orderLineItems = yield renderIntoDocument(
       <LineItems entity={order} model='order'/>
     );
     const renderedDOM = () => ReactDOM.findDOMNode(orderLineItems);
     const editButtons = renderedDOM().querySelectorAll('header .fc-btn');
     const doneButtons = renderedDOM().querySelectorAll('footer .fc-btn');

     expect(editButtons).to.have.length(1);
     expect(doneButtons).to.have.length(0);
     TestUtils.Simulate.click(editButtons[0]);

     const editButtons2 = renderedDOM().querySelectorAll('header .fc-btn');
     const doneButtons2 = renderedDOM().querySelectorAll('footer .fc-btn');
     expect(editButtons2).to.have.length(0);
     expect(doneButtons2).to.have.length(1);
   });
 });
