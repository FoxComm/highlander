
import _ from 'lodash';
import makePagination from '../pagination/flatStore';

const dataNamespace = ['giftCards', 'transactions'];
const { makeReducer, makeActions } = makePagination(dataNamespace);

const reducer = makeReducer();
const { fetch, actionReset } = makeActions(giftCard => `/gift-cards/${giftCard}/transactions`);

export {
  reducer as default,
  fetch,
  actionReset
};
