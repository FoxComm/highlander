
import _ from 'lodash';
import makePagination from '../pagination';

const namespace = 'GIFTCARD_TRANSACTIONS';

const { reducer, fetch, initialFetch, actionReset } = makePagination(
  giftCard => `/gift-cards/${giftCard}/transactions`,
  namespace
);

export {
  reducer as default,
  fetch,
  initialFetch,
  actionReset
};
