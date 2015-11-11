import React from 'react';

describe('Order Shipping Methods', function() {
  const ShippingMethods = requireComponent('orders/order-shipping-method.jsx');

  const defaultProps = {
    order: {
      currentOrder: {
        shippingMethod: {
          name: 'Test Shipping Method',
          price: 3500
        }
      }
    },
    shippingMethods: {
      availableMethods: [
        {
          name: 'Another Shipping Method',
          price: 1230
        },
        {
          name: 'A Third Shipping Method',
          price: 9080
        }
      ]
    }
  };

  it('should render the selected shipping method in the default state', function *() {
    const { container } = yield renderIntoDocument(
      <div><ShippingMethods {...defaultProps} /></div>
    );

    expect(container.querySelector('.fc-shipping-methods')).to.not.equal(null);
    container.unmount();
  });
});
