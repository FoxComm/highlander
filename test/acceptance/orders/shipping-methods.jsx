import React from 'react';

////
// A set of utilities needed for interacting with a table.
const getTableRow = (doc, idx) => {
  const query = `.fc-shipping-methods .fc-table-tbody .fc-table-tr:nth-child(${idx + 1})`;
  return doc.querySelector(query);
};

const getTableCell = (row, idx) => {
  const query = `td:nth-child(${idx + 1})`;
  return row.querySelector(query);
};

const getMethodName = row => {
  const query = '.fc-table-td:nth-child(1)';
  return row.querySelector(query).innerHTML;
};

const getEditableMethodName = row => {
  const query = '.fc-table-td:nth-child(1) .shipping-method-row-name-field';
  return row.querySelector(query).innerHTML;
};

const getMethodPrice = row => {
  const query = '.fc-table-td:nth-child(2) .fc-currency';
  return row.querySelector(query).innerHTML;
};

const getEditableMethodPrice = row => {
  const query = '.fc-table-td:nth-child(2) .shipping-method-input-price input';
  return row.querySelector(query).value;
};

describe('Order Shipping Methods', function() {
  const ShippingMethods = requireComponent('orders/order-shipping-method.jsx');

  const defaultProps = {
    order: {
      currentOrder: {
        shippingMethod: {
          id: 1,
          name: 'Test Shipping Method',
          price: 3500
        }
      }
    },
    shippingMethods: {
      availableMethods: [
        {
          id: 1,
          name: 'Test Shipping Method',
          price: 3500
        },
        {
          id: 2,
          name: 'Another Shipping Method',
          price: 1230
        },
        {
          id: 3,
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
    expect(getMethodName(getTableRow(container, 0))).to.equal('Test Shipping Method');
    container.unmount();
  });

  it('should render the available shipping methods in the edit state', function *() {
    const props = {
      ...defaultProps,
      shippingMethods: {
        ...defaultProps.shippingMethods,
        isEditing: true
      }
    };

    const { container } = yield renderIntoDocument(
      <div><ShippingMethods {...props} /></div>
    );

    const firstRow = getTableRow(container, 0);
    const secondRow = getTableRow(container, 1);
    const thirdRow = getTableRow(container, 2);

    const firstMethodName = getEditableMethodName(firstRow);
    const firstMethodPrice = getMethodPrice(firstRow);
    const secondMethodName = getEditableMethodName(secondRow);
    const secondMethodPrice = getMethodPrice(secondRow);
    const thirdMethodName = getEditableMethodName(thirdRow);
    const thirdMethodPrice = getMethodPrice(thirdRow);

    expect(firstMethodName).to.equal('Test Shipping Method');
    expect(firstMethodPrice).to.equal('$35.00');
    expect(secondMethodName).to.equal('Another Shipping Method');
    expect(secondMethodPrice).to.equal('$12.30');
    expect(thirdMethodName).to.equal('A Third Shipping Method');
    expect(thirdMethodPrice).to.equal('$90.80');
    container.unmount();
  });

  it('should show the edit price box on the selected shipping method when editing price', function *() {
    const props = {
      ...defaultProps,
      shippingMethods: {
        ...defaultProps.shippingMethods,
        isEditing: true,
        isEditingPrice: true
      }
    };

    const { container } = yield renderIntoDocument(
      <div><ShippingMethods {...props} /></div>
    );

    const firstRow = getTableRow(container, 0);
    const secondRow = getTableRow(container, 1);
    const thirdRow = getTableRow(container, 2);

    const firstMethodName = getEditableMethodName(firstRow);
    const firstMethodPrice = getEditableMethodPrice(firstRow);
    const secondMethodName = getEditableMethodName(secondRow);
    const secondMethodPrice = getMethodPrice(secondRow);
    const thirdMethodName = getEditableMethodName(thirdRow);
    const thirdMethodPrice = getMethodPrice(thirdRow);

    expect(firstMethodName).to.equal('Test Shipping Method');
    expect(firstMethodPrice).to.equal('35.00');
    expect(secondMethodName).to.equal('Another Shipping Method');
    expect(secondMethodPrice).to.equal('$12.30');
    expect(thirdMethodName).to.equal('A Third Shipping Method');
    expect(thirdMethodPrice).to.equal('$90.80');
    container.unmount();
  });


  it('should render an empty message when the shipping method is undefined', function *() {
    const emptyProps = {
      order: {
        currentOrder: {}
      },
      shippingMethods: {
        availableMethods: []
      }
    };

    const { container } = yield renderIntoDocument(
      <div><ShippingMethods {...emptyProps} /></div>
    );

    const emptyText = container.querySelector('.fc-content-box-empty-text');
    expect(emptyText).to.not.equal(null);
    expect(emptyText.innerHTML).to.equal('No shipping method applied.');
    container.unmount();
  });

  it('should render an empty message when the shipping method is empty', function *() {
    const emptyProps = {
      order: {
        currentOrder: {
          shippingMethod: {}
        }
      },
      shippingMethods: {
        availableMethods: []
      }
    };

    const { container } = yield renderIntoDocument(
      <div><ShippingMethods {...emptyProps} /></div>
    );

    const emptyText = container.querySelector('.fc-content-box-empty-text');
    expect(emptyText).to.not.equal(null);
    expect(emptyText.innerHTML).to.equal('No shipping method applied.');
    container.unmount();
  });

  it('should render available options when there is no selected shipping method', function *() {
    const props = {
      ...defaultProps,
      order: {
        currentOrder: {
          shippingMethod: {}
        }
      },
      shippingMethods: {
        ...defaultProps.shippingMethods,
        isEditing: true
      }
    };

    const { container } = yield renderIntoDocument(
      <div><ShippingMethods {...props} /></div>
    );

    const firstRow = getTableRow(container, 0);
    const secondRow = getTableRow(container, 1);
    const thirdRow = getTableRow(container, 2);

    const firstMethodName = getEditableMethodName(firstRow);
    const firstMethodPrice = getMethodPrice(firstRow);
    const secondMethodName = getEditableMethodName(secondRow);
    const secondMethodPrice = getMethodPrice(secondRow);
    const thirdMethodName = getEditableMethodName(thirdRow);
    const thirdMethodPrice = getMethodPrice(thirdRow);

    expect(firstMethodName).to.equal('Test Shipping Method');
    expect(firstMethodPrice).to.equal('$35.00');
    expect(secondMethodName).to.equal('Another Shipping Method');
    expect(secondMethodPrice).to.equal('$12.30');
    expect(thirdMethodName).to.equal('A Third Shipping Method');
    expect(thirdMethodPrice).to.equal('$90.80');
    container.unmount();
  });
});
