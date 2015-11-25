import React from 'react';

describe('Order line items', function() {
  const LineItems = requireComponent('orders/order-line-items.jsx');
  const ConfirmationDialog = requireComponent('modal/confirmation-dialog.jsx');

  const defaultProps = {
    order: {
      lineItems: {
        isEditing: true,
        isDeleting: true,
        items: []
      },
      currentOrder: {
      }
    },
    orderLineItemsStartEdit: f => f,
    orderLineItemsCancelEdit: f => f,
    fetchSkus: () => []
  };

  it('should render ConfirmationDialog if isDeleting is truly', function *() {
    const { container } = yield renderIntoDocument(
      <div><LineItems {...defaultProps} /></div>
    );

    expect(container.querySelector('.fc-modal')).to.not.equal(null);
    container.unmount();
  });
});
