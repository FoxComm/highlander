/**
 * @flow
 */

import Api from 'lib/api';
import { dissoc, assoc, update, merge } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import _ from 'lodash';

import { createEmptySku } from 'paragons/sku';
import { actions as imagesActions } from './images';
import { omitAlbumFields } from 'modules/images';

import { pushStockItemChanges } from '../inventory/warehouses';

const defaultContext = 'default';

function cleanSkuPayload(payload: Sku) {
  const nextAlbums = payload.albums.map(album => ({
    ..._.omit(album, omitAlbumFields),
    images: album.images.filter(img => (img.src && img.src.length < 4000))
  }));

  return {
    ...payload,
    albums: nextAlbums,
  };
}

export const skuNew = createAction('SKU_NEW');
const skuClear = createAction('SKU_CLEAR');
export const syncSku = createAction('SKU_SYNC');

const _archiveSku = createAsyncActions(
  'archiveSku',
  (code, context = defaultContext) => {
    return Api.delete(`/skus/${context}/${code}`);
  }
);

export const archiveSku = _archiveSku.perform;
export const clearArchiveErrors = _archiveSku.clearErrors;

const _fetchSku = createAsyncActions(
  'fetchSku',
  (code: string, context: string = defaultContext) => {
    return Api.get(`/skus/${context}/${code}`);
  }
);

const _createSku = createAsyncActions(
  'createSku',
  (sku: Sku, context: string = defaultContext) => {
    return Api.post(`/skus/${context}`, cleanSkuPayload(sku));
  }
);

const _updateSku = createAsyncActions(
  'updateSku',
  function (sku: Sku, context: string = defaultContext) {
    const { dispatch, getState } = this;
    const oldSku = _.get(getState(), ['skus', 'details', 'sku', 'attributes', 'code', 'v']);
    if (oldSku) {
      const stockItemsPromise = dispatch(pushStockItemChanges(oldSku));
      const updatePromise = Api.patch(`/skus/${context}/${oldSku}`, cleanSkuPayload(sku));
      return Promise.all([updatePromise, stockItemsPromise]).then(([updateResponse]) => {
        return updateResponse;
      });
    }
  }
);

export const fetchSku = _fetchSku.perform;
export const createSku = _createSku.perform;
export const updateSku = (sku: Sku, context: string = defaultContext) => (dispatch: Function) => {
  dispatch(imagesActions.clearErrors());

  return dispatch(_updateSku.perform(sku, context));
};

export function clearSubmitErrors() {
  return (dispatch: Function) => {
    dispatch(_createSku.clearErrors());
    dispatch(_updateSku.clearErrors());
  };
}

export const clearFetchErrors = _fetchSku.clearErrors;

export function reset() {
  return (dispatch: Function) => {
    dispatch(skuClear());
    dispatch(clearSubmitErrors());
    dispatch(clearFetchErrors());
  };
}

export type SkuState = {
  sku: ?Sku,
}

const initialState: SkuState = {
  sku: null,
};

function updateSkuInState(state: SkuState, sku: Sku) {
  return {
    ...state,
    sku,
  };
}

const reducer = createReducer({
  [skuNew]: (state) => {
    return assoc(state,
      'sku', createEmptySku(),
      'err', null
    );
  },
  [skuClear]: state => {
    return dissoc(state, 'sku');
  },
  [syncSku]: (state, data) => {
    return update(state, 'sku', merge, data);
  },
  [_createSku.succeeded]: updateSkuInState,
  [_updateSku.succeeded]: updateSkuInState,
  [_fetchSku.succeeded]: updateSkuInState,
}, initialState);

export default reducer;
