const searchTerms = [
  {
    title: 'Gift Card',
    type: 'object',
    options: [
      {
        title: 'Number',
        type: 'term',
        term: 'code'
      },
      {
        title: 'Type',
        type: 'enum',
        term: 'originType',
        suggestions: [
          { display: 'Appeasement', value: 'csrAppeasement' },
          { display: 'Customer Purchase', value: 'customerPurchase' },
          { display: 'From Store Credit', value: 'fromStoreCredit' },
          { display: 'RMA Process', value: 'rmaProcess' }
        ]
      },
      {
        title: 'Original Balance',
        type: 'currency',
        term: 'originalBalance'
      },
      {
        title: 'Current Balance',
        type: 'currency',
        term: 'currentBalance'
      },
      {
        title: 'Available Balance',
        type: 'currency',
        term: 'availableBalance'
      },
      {
        title: 'Status',
        type: 'enum',
        term: 'state',
        suggestions: [
          { display: 'Cart', value: 'cart' },
          { display: 'Active', value: 'active' },
          { display: 'On Hold', value: 'onHold' },
          { display: 'Fully Redeemed', value: 'fullyRedeemed' },
          { display: 'Canceled', value: 'canceled' }
        ]
      }
    ]
  }
];

export default searchTerms;
