/**
 * @flow
 */

import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { push } from 'react-router-redux';
import _ from 'lodash';

import { addIlluminatedAttribute } from '../../paragons/form-shadow-object';
import { createEmptySku } from '../../paragons/sku';

import { pushStockItemChanges } from '../inventory/warehouses';

type Attribute = { t: string, v: any };
type Attributes = { [key:string]: Attribute };

export type Sku = {
  code?: string,
  feCode?: string,
  attributes: Attributes,
  id: any,
  context: {
    attributes?: Object,
    name: string,
  }
};

const defaultContext = 'default';

const requiredFields = {
  upc: 'string',
  description: 'richText',
  retailPrice: 'price',
  salePrice: 'price',
  unitCost: 'price',
};

const skuRequestStart = createAction('SKU_REQUEST_START');
const skuRequestSuccess = createAction('SKU_REQUEST_SUCCESS');
const skuRequestFailure = createAction('SKU_REQUEST_FAILURE');
const skuUpdateStart = createAction('SKU_UPDATE_START');
const skuUpdateSuccess = createAction('SKU_UPDATE_SUCCESS');
const skuUpdateFailure = createAction('SKU_UPDATE_FAILURE');
const setError = createAction('SKU_SET_ERROR');

export const newSku = createAction('SKU_NEW');

export function fetchSku(code: string, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(skuRequestStart());
    return Api.get(`/skus/${context}/${code}`)
      .then(
        (res: Sku) => dispatch(skuRequestSuccess(res)),
        (err: HttpError) => {
          dispatch(skuRequestFailure());
          dispatch(setError(err));
        }
      );
  };
}

export function createSku(sku: Sku, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(skuUpdateStart());
    return Api.post(`/skus/${context}`, sku)
      .then(
        (res: Sku) => {
          const newCode = _.get(res, ['attributes', 'code', 'v']);
          dispatch(skuUpdateSuccess(res));
          dispatch(push(`/skus/${newCode}`));
        },
        (err: HttpError) => {
          dispatch(skuUpdateFailure());
          dispatch(setError(err));
        }
      );
  };
}

export function updateSku(sku: Sku, context: string = defaultContext): ActionDispatch {
  return (dispatch, getState) => {
    const oldSku = _.get(getState(), ['skus', 'details', 'sku', 'attributes', 'code', 'v']);
    if (oldSku) {
      dispatch(skuUpdateStart());
      const stockItemsPromise = dispatch(pushStockItemChanges(oldSku));
      const updatePromise = Api.patch(`/skus/${context}/${oldSku}`, sku)
        .then(
          (res: Sku) => {
            const newCode = _.get(res, ['attributes', 'code', 'v']);
            dispatch(skuUpdateSuccess(res));
            dispatch(push(`/skus/${newCode}`));
          },
          (err: HttpError) => {
            dispatch(skuUpdateFailure());
            dispatch(setError(err));
          }
        );

      return Promise.all([updatePromise, stockItemsPromise]);
    }
  };
}

export type SkuState = {
  err: ?HttpError,
  isFetching: boolean,
  isUpdating: boolean,
  sku: ?Sku,
}

const initialState: SkuState = {
  err: null,
  isFetching: false,
  isUpdating: false,
  sku: null,
};

const reducer = createReducer({
  [newSku]: (state) => {
    return assoc(state, 'sku', createEmptySku());
  },
  [skuRequestStart]: (state: SkuState) => {
    return assoc(state,
      'err', null,
      'isFetching', true
    );
  },
  [skuRequestSuccess]: (state: SkuState, sku: Sku) => {
    const configuredSku = _.reduce(requiredFields, (res, value, key) => {
      const attrs = addIlluminatedAttribute(key, value, null, res.attributes);
      return assoc(sku, 'attributes', attrs);
    }, sku);

    return assoc(state,
      'err', null,
      'isFetching', false,
      'sku', configuredSku,
    );
  },
  [skuRequestFailure]: (state: SkuState) => {
    return assoc(state, 'isFetching', false);
  },
  [skuUpdateStart]: (state: SkuState) => {
    return assoc(state, 'err', null, 'isUpdating', true);
  },
  [skuUpdateSuccess]: (state: SkuState, sku: Sku) => {
    const configuredSku = _.reduce(requiredFields, (res, value, key) => {
      const attrs = addIlluminatedAttribute(key, value, null, res.attributes);
      return assoc(sku, 'attributes', attrs);
    }, sku);

    return assoc(state,
      'err', null,
      'isUpdating', false,
      'sku', configuredSku,
    );
  },
  [skuUpdateFailure]: (state: SkuState) => {
    return assoc(state, 'isUpdating', false);
  },
  [setError]: (state: SkuState, err: Object) => {
    const error: HttpError = {
      status: _.get(err, 'response.status'),
      statusText: _.get(err, 'response.statusText', ''),
      messages: _.get(err, 'response.body.errors', []),
    };

    return assoc(state, 'err', error);
  },
}, initialState);

export default reducer;
