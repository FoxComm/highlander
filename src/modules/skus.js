import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';

export const requestSkus = createAction('SKUS_REQUEST');
export const receiveSkus = createAction('SKUS_RECEIVE');
export const failSkus = createAction('SKUS_FAIL');

const initialState = {
  isFetching: false,
  skus: []
};

export function fetchSkus() {
  return dispatch => {
    dispatch(requestSkus());
    return Api.get('/skus')
      .then(skus => dispatch(receiveSkus(skus)))
      .catch(err => dispatch(failSkus(err)));
  };
}

const reducer = createReducer({
  [requestSkus]: state => {
    return {
      ...state,
      isFetching: true
    };
  },
  [receiveSkus]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      skus: payload
    };
  },
  [failSkus]: (state, err) => {
    console.error(err);

    return {
      ...state,
      isFetching: false
    };
  }
}, initialState);

export default reducer;
