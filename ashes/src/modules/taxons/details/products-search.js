/* @flow */

// libs
import _ from 'lodash';
import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { searchProducts } from 'elastic/products';

/*
 *  Internal actions
 */
const _search = createAsyncActions('searchProducts', searchProducts);

/*
 *  External actions
 */
export const search = _search.perform;

/*
 *  Reducer
 */
const initialState = [];

export default createReducer({
  [_search.succeeded]: (state, response) => _.isEmpty(response.result) ? [] : response.result,
}, initialState);
