
// import _ from 'lodash';
import makeLiveSearch from '../live-search';

// const namespace = 'GIFTCARD_TRANSACTIONS';

// const { reducer, fetch, initialFetch, actionReset } = makePagination(
//   giftCard => `/gift-cards/${giftCard}/transactions`,
//   namespace
// );
const searchTerms = [];

const searches = [];

const { reducer, actions } = makeLiveSearch('giftCardTransactions', searchTerms, searches);

export {
  reducer as default,
  actions
};
