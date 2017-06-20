// TODO: This is about rendering line items on carts.
//       Re-enable this when adding cart functionality back.
// import React from 'react';
//
// describe('Order line items', function() {
//   const { OrderLineItems } = requireComponent('orders/order-line-items.jsx', false);
//   const ConfirmationModal = requireComponent('core/confirmation-modal.jsx');
//
//   const defaultProps = {
//     lineItems: {
//       isEditing: true,
//       isDeleting: true,
//       items: []
//     },
//     order: {
//       currentOrder: {}
//     },
//     orderLineItemsStartEdit: f => f,
//     orderLineItemsCancelEdit: f => f,
//     fetchSkus: () => [],
//     isCart: true,
//   };
//
//   it('should render ConfirmationModal if isDeleting is truly', function *() {
//     const { container } = yield renderIntoDocument(
//       <div><OrderLineItems {...defaultProps} /></div>
//     );
//
//     expect(container.querySelector('.fc-modal')).to.not.equal(null);
//     container.unmount();
//   });
// });
