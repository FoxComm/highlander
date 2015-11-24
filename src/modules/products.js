import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';

export const requestProducts = createAction('PRODUCTS_REQUEST');
export const receiveProducts = createAction('PRODUCTS_RECEIVE');
export const failProducts = createAction('PRODUCTS_FAIL');

const initialState = {
  isFetching: false,
  products: []
};

export function fetchProducts() {
  return dispatch => {
    dispatch(requestProducts());
    return Api.get('/products')
      .then(order => dispatch(receiveProducts(products)))
      .catch(err => dispatch(failProducts(err)));
  };
}

const reducer = createReducer({
  [requestProducts]: state => {
    return {
      ...state,
      isFetching: true
    };
  },
  [receiveProducts]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      products: payload
    };
  },
  [failProducts]: (state, err) => {
    console.error(err);

    return {
      ...state,
      isFetching: false
    };
  }
}, initialState);

export default reducer;
