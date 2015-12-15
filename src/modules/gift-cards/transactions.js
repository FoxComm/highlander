
import _ from 'lodash';
import { paginateReducer } from '../pagination';
import { createAction, createReducer } from 'redux-act';
import makePagination from '../pagination';

const namespace = 'GIFT_CARD_TRANSACTIONS';

export const setGiftCard = createAction(`${'GIFT_CARD_TRANSACTIONS'}_SET_GIFT_CARD`);

const moduleReducer = createReducer({
  [setGiftCard]: (state, giftCard) => {
    return {
      ...state,
      giftCard,
    };
  },
});

const {
  reducer,
  actions: {
    fetch,
    setFetchParams,
    actionReset
    }
  } = makePagination(data => `/gift-cards/${data.giftCard}/transactions`, namespace, moduleReducer);

export {
  reducer as default,
  fetch,
  setFetchParams,
  actionReset
};
