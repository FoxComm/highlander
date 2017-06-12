
import { dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import { createEmptyPromotion } from 'paragons/promotion';
import { createAsyncActions } from '@foxcomm/wings';
import Api from 'lib/api';

export const contentTypeNew = createAction('CONTENT_TYPES_NEW');
const clearContentType = createAction('CONTENT_TYPE_CLEAR');
const defaultContext = 'default';

const _fetchContentType = createAsyncActions(
  'fetchContentType',
  (id: string, context) => {
    return Api.get(`/promotions/${context}/${id}`);
  }
);

const _createContentType = createAsyncActions(
  'createContentType',
  (promotion, context = defaultContext) => {
    return Api.post(`/promotions/${context}`, promotion);
  }
);

const _updateConentType = createAsyncActions(
  'updateConentType',
  (promotion, context = defaultContext) => {
    const id = promotion.id;
    return Api.patch(`/promotions/${context}/${id}`, promotion);
  }
);

export function clearSubmitErrors() {
  return dispatch => {
    dispatch(_createContentType.clearErrors());
    dispatch(_updateConentType.clearErrors());
  };
}

export function fetchContentType(id: string, context: string = defaultContext) {
  return dispatch => {
    if (id.toLowerCase() == 'new') {
      dispatch(contentTypeNew());
    } else {
      return dispatch(_fetchContentType.perform(id, context));
    }
  };
}

const _archiveContentType = createAsyncActions(
  'archiveContentType',
  (id, context = defaultContext) => {
    return Api.delete(`/promotions/${context}/${id}`);
  }
);

export const clearArchiveErrors = _archiveContentType.clearErrors;
export const archiveContentType = _archiveContentType.perform;
export const createContentType = _createContentType.perform;
export const updateConentType = _updateConentType.perform;
export const clearFetchErrors = _fetchContentType.clearErrors;

export function reset() {
  return dispatch => {
    dispatch(clearContentType());
    dispatch(clearSubmitErrors());
    dispatch(clearFetchErrors());
  };
}

function updateConentTypeInState(state, response) {
  return {
    ...state,
    promotion: response
  };
}

const initialState = {
  promotion: null,
};

const reducer = createReducer({
  [contentTypeNew]: state => {
    return {
      ...state,
      promotion: createEmptyPromotion(),
    };
  },
  [clearContentType]: state => {
    return dissoc(state, 'promotion');
  },
  [_fetchContentType.succeeded]: updateConentTypeInState,
  [_createContentType.succeeded]: updateConentTypeInState,
  [_updateConentType.succeeded]: updateConentTypeInState,
}, initialState);

export default reducer;
