/**
 * @flow
 */

import { createAction, createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';

const defaultContext = 'default';

const _fetchOptions = createAsyncActions(
  'fetchOptions',
  (id, context = defaultContext) => {
    return Api.get(`/variants/${context}/${id}`);
  }
);

const _addOptions = createAsyncActions(
  'addOptions',
  (id, context = defaultContext) => {
    return Api.get(`/variants/${context}/${id}`);
  }
);

const _updateOptions = createAsyncActions(
  'archiveProduct',
  (id, context = defaultContext) => {
    return Api.get(`/variants/${context}/${id}`);
  }
);
