
import _ from 'lodash';
import makePagination from '../pagination/flatStore';

const {
  reducer,
  actions: {
    fetch,
    actionReset
    }
  } = makePagination(['giftCards', 'transactions'])(giftCard => `/gift-cards/${giftCard}/transactions`);

export {
  reducer as default,
  fetch,
  actionReset
};
