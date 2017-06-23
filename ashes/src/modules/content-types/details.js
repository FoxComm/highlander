
import { dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import { createEmptyContentType } from 'paragons/content-type';
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
  (contentType, context = defaultContext) => {
    return Api.post(`/promotions/${context}`, contentType);
  }
);

const _updateContentType = createAsyncActions(
  'updateContentType',
  (contentType, context = defaultContext) => {
    const id = contentType.id;
    return Api.patch(`/promotions/${context}/${id}`, contentType);
  }
);

export function clearSubmitErrors() {
  return dispatch => {
    dispatch(_createContentType.clearErrors());
    dispatch(_updateContentType.clearErrors());
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
export const updateContentType = _updateContentType.perform;
export const clearFetchErrors = _fetchContentType.clearErrors;

export function reset() {
  return dispatch => {
    dispatch(clearContentType());
    dispatch(clearSubmitErrors());
    dispatch(clearFetchErrors());
  };
}

function updateContentTypeInState(state, response) {
  return {
    ...state,
    contentType: response
  };
}

const initialState = {
  contentType: null,
};

const reducer = createReducer({
  [contentTypeNew]: state => {
    return {
      ...state,
      contentType: createEmptyContentType(),
    };
  },
  [clearContentType]: state => {
    return dissoc(state, 'contentType');
  },
  [_fetchContentType.succeeded]: updateContentTypeInState,
  [_createContentType.succeeded]: updateContentTypeInState,
  [_updateContentType.succeeded]: updateContentTypeInState,
}, initialState);

export default reducer;
