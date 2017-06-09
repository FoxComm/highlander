const searchTerms = [
  {
    title: 'Content Type : Number',
    type: 'identifier',
    term: 'code'
  },
  {
    title: 'Content Type : Type',
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
    title: 'Content Type : Original Balance',
    type: 'currency',
    term: 'originalBalance'
  },
  {
    title: 'Content Type : Current Balance',
    type: 'currency',
    term: 'currentBalance'
  },
  {
    title: 'Content Type : Available Balance',
    type: 'currency',
    term: 'availableBalance'
  },
  {
    title: 'Content Type : Status',
    type: 'enum',
    term: 'state',
    suggestions: [
      { display: 'Cart', value: 'cart' },
      { display: 'Active', value: 'active' },
      { display: 'On Hold', value: 'onHold' },
      { display: 'Fully Redeemed', value: 'fullyRedeemed' },
      { display: 'Canceled', value: 'canceled' }
    ]
  },
  {
    title: 'Content Type : Date/Time Issued',
    type: 'date',
    term: 'createdAt'
  },
];

export default searchTerms;
