/**
 * @flow
 */

import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { pushState } from 'redux-router';
import _ from 'lodash';

import { addAttribute } from '../../paragons/form-shadow-object';

const defaultContext = 'default';

const requiredFields = {
  sku: 'string',
  upc: 'string',
  description: 'richText',
  retailPrice: 'price',
  salePrice: 'price',
  unitCost: 'price',
};

const skuRequestStart = createAction('SKU_REQUEST_START');
const skuRequestSuccess = createAction('SKU_REQUEST_SUCCESS');
const skuRequestFailure = createAction('SKU_REQUEST_FAILURE');
const setError = createAction('SKU_SET_ERROR');

export function fetchSku(code: string, context: string = defaultContext): ActionDispatch {
  return dispatch => {
    dispatch(skuRequestStart());
    return Api.get(`/skus/full/${context}/${code}`)
      .then(
        (res: FullSku) => dispatch(skuRequestSuccess(res)),
        (err: HttpError) => {
          dispatch(skuRequestFailure());
          dispatch(setError(err));
        }
      );
  };
}

type SkuForm = {
  code: string,
  attributes: FormAttributes,
};

type SkuShadow = {
  code: string,
  attributes: ShadowAttributes,
};

export type FullSku = {
  code: string,
  form: SkuForm,
  shadow: SkuShadow,
};

export type SkuState = {
  err: ?HttpError,
  isFetching: boolean,
  sku: ?FullSku,
}

const initialState: SkuState = {
  err: null,
  isFetching: false,
  sku: null,
};

const reducer = createReducer({
  [skuRequestStart]: (state: SkuState) => {
    return assoc(state,
      'err', null,
      'isFetching', true
    );
  },
  [skuRequestSuccess]: (state: SkuState, sku: FullSku) => {
    const configuredSku = _.reduce(requiredFields, (res, value, key) => {
      const { form, shadow } = addAttribute(key, value, null, res.form.attributes, res.shadow.attributes);
      return assoc(sku,
        ['form', 'attributes'], form,
        ['shadow', 'attributes'], shadow
      );
    }, sku);

    return assoc(state,
      'err', null,
      'isFetching', false,
      'sku', configuredSku
    );
  },
  [skuRequestFailure]: (state: SkuState) => {
    return assoc(state, 'isFetching', false);
  },
  [setError]: (state: SkuState, err: Object) => {
    const error: HttpError = {
      status: _.get(err, 'response.status'),
      statusText: _.get(err, 'response.statusText', ''),
      messages: _.get(err, 'responseJson.error', []),
    };

    return assoc(state, 'err', error);
  },
}, initialState);

export default reducer;
