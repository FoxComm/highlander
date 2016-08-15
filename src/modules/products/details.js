/**
 * @flow
 */

// libs
import _ from 'lodash';
import { update } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { push } from 'react-router-redux';
import reduceReducers from 'reduce-reducers';
import { post } from 'lib/search';
import Api from 'lib/api';
import { createEmptyProduct, configureProduct } from 'paragons/product';
import * as dsl from 'elastic/dsl';
import createAsyncActions from 'modules/async-utils';

// types
import type { Product } from 'paragons/product';

export type Error = {
  status: ?number,
  statusText: ?string,
  messages: Array<string>,
};

export type ProductDetailsState = {
  err: ?Error,
  isFetching: boolean,
  isUpdating: boolean,
  product: ?Product,
};

const _suggestSkus = createAsyncActions(
  'products-suggestSkus',
  (context: string, code: string) => {
    return post('sku_search_view/_search', dsl.query({
      bool: {
        // filter: [
        //   dsl.termFilter('context', context),
        // ],
        must: [
          dsl.matchQuery('_all', {
            query: code,
          }),
        ]
      },
    }));
  }
);

export const suggestSkus = _suggestSkus.perform;

const defaultContext = 'default';

const productRequestStart = createAction('PRODUCTS_REQUEST_START');
const productRequestSuccess = createAction('PRODUCTS_REQUEST_SUCCESS');
const productRequestFailure = createAction('PRODUCTS_REQUEST_FAILURE');

const productUpdateStart = createAction('PRODUCTS_UPDATE_START');
const productUpdateSuccess = createAction('PRODUCTS_UPDATE_SUCCESS');
const productUpdateFailure = createAction('PRODUCTS_UPDATE_FAILURE');

export const productNew = createAction('PRODUCTS_NEW');
const productSet = createAction('PRODUCTS_SET');

const setError = createAction('PRODUCTS_SET_ERROR');

function sanitizeError(error: string): string {
  if (error.indexOf('sku_code violates check constraint "sku_code_check"') != -1) {
    return 'Product must contain a SKU.';
  }

  return error;
}

export function fetchProduct(id: string, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    if (id.toLowerCase() == 'new') {
      dispatch(productNew());
    } else {
      dispatch(productRequestStart());
      return Api.get(`/products/${context}/${id}`)
        .then(
          (product: Product) => dispatch(productRequestSuccess(product)),
          (err: Object) => {
            dispatch(productRequestFailure());
            dispatch(setError(err));
          }
        );
    }
  };
}

export function createProduct(product: Product, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(productSet(product));
    dispatch(productUpdateStart());
    return Api.post(`/products/${context}`, product)
      .then(
        (product: Product) => {
          dispatch(productUpdateSuccess(product));
          dispatch(push(`/products/${context}/${product.id}`));
        },
        (err: Object) => {
          dispatch(productUpdateFailure());
          dispatch(setError(err));
        }
      );
  };
}

export function updateProduct(product: Product, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(productSet(product));
    dispatch(productUpdateStart());
    return Api.patch(`/products/${context}/${product.id}`, product)
      .then(
        (product: Product) => dispatch(productUpdateSuccess(product)),
        (err: Object) => {
          dispatch(productUpdateFailure());
          dispatch(setError(err));
        }
      );
  };
}

const initialState: ProductDetailsState = {
  err: null,
  isFetching: false,
  isUpdating: false,
  product: null,
  response: null,
  suggestedSkus: [],
};

const reducer = createReducer({
  [productNew]: (state: ProductDetailsState) => {
    return {
      ...initialState,
      product: createEmptyProduct(),
    };
  },
  [productSet]: (state: ProductDetailsState, product: Product) => {
    return {
      ...initialState,
      product,
    };
  },
  [productRequestStart]: (state: ProductDetailsState) => {
    return {
      ...state,
      err: null,
      isFetching: true,
    };
  },
  [productRequestSuccess]: (state: ProductDetailsState, response: Product) => {
    return {
      ...state,
      err: null,
      isFetching: false,
      product: configureProduct(response),
    };
  },
  [productRequestFailure]: (state: ProductDetailsState) => {
    return {
      ...state,
      isFetching: false,
    };
  },
  [productUpdateStart]: (state: ProductDetailsState) => {
    return {
      ...state,
      isUpdating: true,
    };
  },
  [productUpdateSuccess]: (state: ProductDetailsState, response: Product) => {
    return {
      ...state,
      err: null,
      isUpdating: false,
      product: configureProduct(response),
    };
  },
  [productUpdateFailure]: (state: ProductDetailsState) => {
    return {
      ...state,
      isUpdating: false,
    };
  },
  [_suggestSkus.succeeded]: (state, response) => {
    return {
      ...state,
      suggestedSkus: _.get(response, 'result', [])
    };
  },
  [setError]: (state: ProductDetailsState, err: Object) => {
    const messages = _.get(err, 'response.body.errors', []);

    const error: Error = {
      status: _.get(err, 'response.status'),
      statusText: _.get(err, 'response.statusText', ''),
      messages: _.map(messages, m => sanitizeError(m)),
    };

    return {
      ...state,
      err: error,
    };
  },
}, initialState);


export default reducer;
