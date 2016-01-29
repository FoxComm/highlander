
import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Adjustment',
    type: 'object',
    options: [
      {
        title: 'ID',
        type: 'string',
        term: 'id'
      },
      {
        title: 'Created At',
        type: 'date',
        term: 'createdAt'
      },
      {
        title: 'Debit',
        type: 'currency',
        term: 'debit'
      },
      {
        title: 'Credit',
        type: 'currency',
        term: 'credit'
      },
      {
        title: 'Available Balance',
        type: 'currency',
        term: 'availableBalance'
      },
    ]
  },
  {
    title: 'Order',
    type: 'object',
    options: [
      {
        title: 'Reference Number',
        type: 'string',
        term: 'orderReferenceNumber'
      },
      {
        title: 'Created At',
        type: 'date',
        term: 'orderCreatedAt'
      },
      {
        title: 'Payment Created At',
        type: 'date',
        term: 'orderPaymentCreatedAt'
      },
    ]
  }
];

const { reducer, actions } = makeLiveSearch(
  'giftCards-transactions',
  searchTerms,
  'gift_card_transactions_view/_search',
  'giftCardTransactionsScope'
);

export {
  reducer as default,
  actions
};
