// @flow

import { assoc } from 'sprout-data';
import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcommerce/wings';

const _fetchInventorySummary = createAsyncActions('fetchInventorySummary',
  function(skuCode: string) {
    // const { api } = this;

    // return api.get(`/v1/public/summary/${skuCode}`);
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          skuCode,
          onHand: 2,
        });
      }, 400);
    });
  },
  (...args: Array<any>) => args
);

export const fetchInventorySummary = _fetchInventorySummary.perform;

const initialState = {
  summary: {},
};

const reducer = createReducer({
  [_fetchInventorySummary.succeeded]: (state, [response, skuCode]) => {
    return assoc(state, ['summary', skuCode], response);
  },
}, initialState);

export default reducer;
