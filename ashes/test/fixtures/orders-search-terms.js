module.exports = [
  {
    title: 'Order',
    type: 'object',
    options: [
      {
        title: 'ID',
        type: 'string',
      },
      {
        title: 'State',
        type: 'enum',
        suggestions: [
          'Cart',
          'Remorse Hold',
          'Manual Hold',
          'Fraud Hold',
          'Fulfillment Started',
          'Shipped',
          'Partially Shipped',
          'Canceled',
        ],
      },
      {
        title: 'Date Placed',
        type: 'date',
      },
    ],
  },
  {
    title: 'Shipment',
    type: 'object',
    options: [
      {
        title: 'State',
        type: 'enum',
        suggestions: ['Shipped', 'Partially Shipped', 'Delivered'],
      },
      {
        title: 'Method',
        type: 'string',
      },
      {
        title: 'Cost',
        type: 'number',
      },
      {
        title: 'Total Number',
        type: 'number',
      },
    ],
  },
];
