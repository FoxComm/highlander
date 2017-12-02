/* @flow */

// libs
import _ from 'lodash';
import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcommerce/wings';
import { searchCustomers } from 'elastic/customers';

const _suggestCustomers = createAsyncActions('suggestCustomers', searchCustomers);

export const suggestCustomers = (excludeCustomers: Array<number>) => {
  return _suggestCustomers.perform.bind(null, excludeCustomers);
};

const initialState = {
  customers: [],
};

const reducer = createReducer({
  [_suggestCustomers.succeeded]: (state, response) => {
    return {
      ...state,
      customers: _.isEmpty(response.result) ? [] : response.result,
    };
  },
}, initialState);

export default reducer;
