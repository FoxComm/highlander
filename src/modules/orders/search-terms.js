const searchTerms = [
  {
    title: 'Order',
    type: 'object',
    options: [
      {
        title: 'ID',
        type: 'string',
        term: 'id'
      }, {
        title: 'State',
        type: 'enum',
        term: 'status',
        suggestions: [
          { display: 'Cart', value: 'cart' },
          { display: 'Remorse Hold', value: 'remorseHold' },
          { display: 'Manual Hold', value: 'manualHold' },
          { display: 'Fraud Hold', value: 'fraudHold' },
          { display: 'Fulfillment Started', value: 'fulfillmentStarted' },
          { display: 'Shipped', value: 'shipped' },
          { display: 'Partially Shipped', value: 'partiallyShipped' },
          { display: 'Canceled', value: 'canceled' }
        ]
      }, {
        title: 'Date Placed',
        type: 'date',
        term: 'placedAt'
      }
    ]
  }, {
    title: 'Shipment',
    type: 'object',
    options: [
      {
        title: 'State',
        type: 'enum',
        suggestions: [
          { display: 'Shipped', value: '' },
          { display: 'Partially Shipped', value: '' },
          { display: 'Delivered', value: '' }
        ]
      }, {
        title: 'Method',
        type: 'string'
      }, {
        title: 'Cost',
        type: 'number'
      }, {
        title: 'Total Number',
        type: 'number'
      }
    ]
  }
];

export default searchTerms;
