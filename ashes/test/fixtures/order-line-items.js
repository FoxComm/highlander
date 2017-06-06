const ordersSearchOptions = [
  {
    term: 'Order',
    type: 'object',
    options: [
      {
        term: 'ID',
        type: 'string',
      },
      {
        term: 'State',
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
        term: 'Date Placed',
        type: 'date',
      },
    ],
  },
  {
    term: 'Shipment',
    type: 'object',
    options: [
      {
        term: 'State',
        type: 'enum',
        suggestions: ['Shipped', 'Partially Shipped', 'Delivered'],
      },
      {
        term: 'Method',
        type: 'string',
      },
      {
        term: 'Cost',
        type: 'number',
      },
      {
        term: 'Total Number',
        type: 'number',
      },
    ],
  },
];

export default ordersSearchOptions;
