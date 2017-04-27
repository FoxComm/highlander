import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Order',
    type: 'object',
    options: [
      {
        title: 'Reference Number',
        type: 'identifier',
        term: 'referenceNumber'
      },
      {
        title: 'State',
        type: 'enum',
        term: 'state',
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
      },
      {
        title: 'Date Placed',
        type: 'date',
        term: 'placedAt'
      },
      {
        title: 'Total',
        type: 'currency',
        term: 'grandTotal'
      }
    ]
  },
  {
    title: 'Payment',
    type: 'object',
    options: [
      {
        title: 'Credit Card Total',
        type: 'currency',
        term: 'creditCardTotal'
      }, {
        title: 'Credit Card Total Number',
        type: 'number',
        term: 'creditCardCount'
      }, {
        title: 'Gift Card Total',
        type: 'currency',
        term: 'giftCardTotal'
      }, {
        title: 'Gift Card Total Number',
        type: 'number',
        term: 'giftCardCount'
      }, {
        title: 'Store Credit Total',
        type: 'currency',
        term: 'storeCreditTotal'
      }, {
        title: 'Store Credit Total Number',
        type: 'number',
        term: 'storeCreditCount'
      }
    ]
  },
  {
    title: 'Items',
    type: 'object',
    options: [
      {
        title: 'Total Number',
        type: 'number',
        term: 'lineItemCount'
      }, {
        title: 'Product Name',
        type: 'string',
        term: 'lineItems.name'
      }, {
        title: 'Product SKU',
        type: 'identifier',
        term: 'lineItems.sku'
      }
    ]
  }, {
    title: 'Assignee',
    type: 'enum',
    term: 'assignmentCount',
    suggestions: [
      { display: 'Has Assignee', operator: 'gt', value: 0 },
      { display: 'Has No Assignee', operator: 'eq', value: 0 }
    ]
  }
];

const { reducer, actions } = makeLiveSearch(
  'customers.transactions',
  searchTerms,
  'orders_search_view/_search',
  'customerTransactionsScope', {
    initialState: { sortBy: '-placedAt' },
    skipInitialFetch: true
  }
);

export {
  reducer as default,
  actions
};
