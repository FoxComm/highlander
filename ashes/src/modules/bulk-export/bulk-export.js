/* @flow */

import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';
import localStorage from 'localStorage';
import _ from 'lodash';
import { columnsToPayload } from './helpers';
import { createAction, createReducer } from 'redux-act';

type Payload = {
  ids?: Array<number>,
  query?: Object,
  fields: Array<Object>,
  description?: string,
};

export const saveRawQuery = createAction('SAVE_RAW_QUERY');

const getFields = (allFields: Array<Object>, identifier: string): Array<Object> => {
  const columns = localStorage.getItem('columns') ? JSON.parse(localStorage.getItem('columns')) : {};

  if (_.isEmpty(columns[identifier])) {
    return allFields;
  }

  const selectedFields = _.filter(columns[identifier], column => column.isVisible === true);
  return columnsToPayload(selectedFields);
};

const getQuery = (raw: Object): Object => {
  if (_.isEmpty(raw.query)) {
    return {
      query: {
        bool: {},
      },
      sort: raw.sort,
    };
  }
  return {
    ...raw,
  };
};

const genDownloadLink = (response: Object) => {
  const blob = new Blob([response.data]);
  const link = document.createElement('a');
  link.href=window.URL.createObjectURL(blob);
  link.download=response.fileName;
  link.click();
};

export const bulkExport = createAsyncActions(
  'bulkExport',
  function(fields, entity, identifier, description) {
    const { getState } = this;
    const queryFields = getFields(fields, identifier);
    const rawQuery = _.get(getState().bulkExport, 'rawQuery', {});
    const queries = getQuery(rawQuery);

    const payload: Payload = {
      fields: queryFields,
      ...queries,
    };
    if (description != null) payload.description = description;

    return Api.post(`/export/${entity}`, {
      payloadType: 'query',
      ...payload,
    }).then((res) => {
      genDownloadLink(res);
    });
  }
).perform;

export const bulkExportByIds = createAsyncActions(
  'bulkExportByIds',
  function(ids, description, fields, entity, identifier) {
    const queryFields = getFields(fields, identifier);
    const payload: Payload = {
      ids,
      fields: queryFields,
    };
    if (description != null) payload.description = description;

    return Api.post(`/export/${entity}`, {
      payloadType: 'ids',
      ...payload,
    }).then((res) => {
      genDownloadLink(res);
    });
  }
).perform;

const initialState = {
  rawQuery: {},
};

const reducer = createReducer({
  [saveRawQuery]: (state, rawQuery) => {
    return {
      ...state,
      rawQuery,
    };
  },
}, initialState);

export default reducer;
