
import _ from 'lodash';
import makePagination from '../pagination';

const namespace = 'GIFTCARD_TRANSACTIONS';

const { reducer, fetch, actionReset } = makePagination(
  giftCard => `/gift-cards/${giftCard}/transactions`,
  namespace
);

export {
  reducer as default,
  fetch,
  actionReset
};
