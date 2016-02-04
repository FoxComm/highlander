const searchTerms = [
  {
    title: 'Customer',
    type: 'object',
    options: [
      {
        title: 'Name',
        type: 'string',
        term: 'name'
      }, {
        title: 'Email',
        type: 'string',
        term: 'email'
      }, {
        title: 'Revenue',
        type: 'currency',
        term: 'revenue'
      }, {
        title: 'Ranking',
        type: 'number',
        term: 'ranking'
      }, {
        title: 'Date/Time Joined',
        type: 'date',
        term: 'joinedAt'
      }
    ]
  }, {
    title: 'Order',
    type: 'object',
    options: [
      {
        title: 'Reference Number',
        type: 'string',
        term: 'orders.referenceNumber'
      }, {
        title: 'State',
        type: 'enum',
        term: 'orders.state',
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
        term: 'orders.placedAt'
      }, {
        title: 'Total',
        type: 'currency',
        term: 'orders.grandTotal'
      }
    ]
  }, {
    title: 'Shipping',
    type: 'object',
    options: [
      {
        title: 'City',
        type: 'string',
        term: 'shippingAddresses.city'
      }, {
        title: 'State',
        type: 'string',
        term: 'shippingAddresses.region'
      }, {
        title: 'Zip',
        type: 'string',
        term: 'shippingAddresses.zip'
      }
    ]
  }, {
    title: 'Billing',
    type: 'object',
    options: [
      {
        title: 'City',
        type: 'string',
        term: 'billingAddresses.city'
      }, {
        title: 'State',
        type: 'string',
        term: 'billingAddresses.region'
      }, {
        title: 'Zip',
        type: 'string',
        term: 'billingAddresses.zip'
      }
    ]
  }
];

export default searchTerms;
