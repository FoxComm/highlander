/* @flow */
import _ from 'lodash';
import Api from 'lib/api';
import { createAction, createReducer } from 'redux-act';

import createAsyncActions from 'modules/async-utils';

import type { ShippingMethod, CreatePayload, UpdatePayload } from 'paragons/shipping-method';

type State = {
  shippingMethod: {
    id?: number,
    attributes?: ShippingMethod,
  },
};

const _fetchShippingMethod = createAsyncActions(
  'fetchShippingMethod',
  (id: number) => Api.get(`/shipping-methods/${id}`)
);

const _createShippingMethod = createAsyncActions(
  'createShippingMethod',
  (payload: CreatePayload) => Api.post('/shipping-methods', payload)
);

const _updateShippingMethod = createAsyncActions(
  'updateShippingMethod',
  (id: number, payload: UpdatePayload) => Api.patch(`/shipping-methods/${id}`, payload)
);

const _archiveShippingMethod = createAsyncActions(
  'archiveShippingMethod',
  (id: number) => Api.delete(`/shipping-methods/${id}`)
);

const clearShippingMethod = createAction('SHIPPING_METHOD_CLEAR');
export const shippingMethodNew = createAction('SHIPPING_METHOD_NEW');
export const createShippingMethod = _createShippingMethod.perform;
export const updateShippingMethod = _updateShippingMethod.perform;
export const archiveShippingMethod = _archiveShippingMethod.perform;
export const clearFetchErrors = _fetchShippingMethod.clearErrors;
export const clearArchiveErrors = _archiveShippingMethod.clearErrors;

export function fetchShippingMethod(id: string): ActionDispatch {
  return (dispatch: Function) => {
    if (id.toLowerCase() == 'new') {
      return dispatch(shippingMethodNew());
    } else {
      return dispatch(_fetchShippingMethod.perform(id));
    }
  };
}

export function clearSubmitErrors() {
  return (dispatch: Function) => {
    dispatch(_createShippingMethod.clearErrors());
    dispatch(_updateShippingMethod.clearErrors());
    dispatch(_archiveShippingMethod.clearErrors());
  };
}

export function reset() {
  return (dispatch: Function) => {
    dispatch(clearShippingMethod());
    dispatch(clearFetchErrors());
  };
}

const initialState: State = {
  shippingMethod: {},
};

function receiveShippingMethod(state: State, shippingMethod: ShippingMethod): State {
  const method = { 
    id: shippingMethod.id,
    attributes: shippingMethod,
  };

  return { ...state, shippingMethod: method };
}

const reducer = createReducer({
  [_fetchShippingMethod.succeeded]: receiveShippingMethod,
  [_createShippingMethod.succeeded]: receiveShippingMethod,
  [_updateShippingMethod.succeeded]: receiveShippingMethod,
  [clearShippingMethod]: state => ({ ...state, shippingMethod: {} }),
}, initialState);

export default reducer;
