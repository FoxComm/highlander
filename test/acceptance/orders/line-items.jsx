import React from 'react';

describe('Order line items', function() {
  const { OrderLineItems } = requireComponent('orders/order-line-items.jsx');
  const ConfirmationDialog = requireComponent('modal/confirmation-dialog.jsx');

  const defaultProps = {
    lineItems: {
      isEditing: true,
      isDeleting: true,
      items: []
    },
    order: {
      currentOrder: {}
    },
    orderLineItemsStartEdit: f => f,
    orderLineItemsCancelEdit: f => f,
    fetchSkus: () => []
  };

  it('should render ConfirmationDialog if isDeleting is truly', function *() {
    const { container } = yield renderIntoDocument(
      <div><OrderLineItems {...defaultProps} /></div>
    );

    expect(container.querySelector('.fc-modal')).to.not.equal(null);
    container.unmount();
  });
});
