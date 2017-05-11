/* @flow */

import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';
import localStorage from 'localStorage';
import _ from 'lodash';
import { columnsToPayload } from './helpers';

type Payload = {
  ids: Array<number>,
  fields: Array<Object>,
  description?: string,
};

const getFields = (allFields: Array<Object>, identifier: string): Array<Object> => {
  const columns = localStorage.getItem('columns') ? JSON.parse(localStorage.getItem('columns')) : {};

  if (_.isEmpty(columns[identifier])) {
    return allFields;
  }

  const selectedFields = _.filter(columns[identifier], column => column.isVisible === true);
  return columnsToPayload(selectedFields);
};

const getQuery = (raw: Object, sort: Array<Object>): Object => {
  if (_.isEmpty(raw)) {
    return {
      query: {
        bool: {},
      },
      sort,
    };
  }
  return {
    query: raw.query,
    sort,
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
  function(fields, entity, identifier, description, sort) {
    const { getState } = this;
    const queryFields = getFields(fields, identifier);
    const selectedSearch = getState()[entity].list.selectedSearch;
    const savedSearch = getState()[entity].list.savedSearches[selectedSearch];
    const queries = getQuery(savedSearch.rawQuery, sort);

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
