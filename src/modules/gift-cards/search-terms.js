const searchTerms = [
  {
    title: 'Gift Card : Number',
    type: 'string',
    term: 'code'
  },
  {
    title: 'Gift Card : Type',
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
    title: 'Gift Card : Original Balance',
    type: 'currency',
    term: 'originalBalance'
  },
  {
    title: 'Gift Card : Current Balance',
    type: 'currency',
    term: 'currentBalance'
  },
  {
    title: 'Gift Card : Available Balance',
    type: 'currency',
    term: 'availableBalance'
  },
  {
    title: 'Gift Card : Status',
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
];

export default searchTerms;
