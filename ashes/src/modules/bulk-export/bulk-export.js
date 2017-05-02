import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';
import localStorage from 'localStorage';
import _ from 'lodash';

const getFields = (allFields, identifier) => {
  const columns = localStorage.getItem('columns') ? JSON.parse(localStorage.getItem('columns')) : {};

  if (_.isEmpty(columns[identifier])) {
    return allFields;
  }
  return _.filter(columns[identifier], {isVisible: true}).map(c => c.field);


};

const getQuery = (raw) => {
  if (_.isEmpty(raw)) {
    return {
      bool: {},
    };
  }
  return raw.query;
};

const genDownloadLink = (response) => {
  const blob = new Blob([response.data]);
  const link = document.createElement('a');
  link.href=window.URL.createObjectURL(blob);
  link.download=response.fileName;
  link.click();
};

export const bulkExport = createAsyncActions(
  'bulkExport',
  function(fields, entity, identifier) {
    const { getState } = this;
    const queryFields = getFields(fields, identifier);
    const selectedSearch = getState()[entity].list.selectedSearch;
    const savedSearch = getState()[entity].list.savedSearches[selectedSearch];
    const query = getQuery(savedSearch.rawQuery);

    return Api.post(`/export/${entity}`, {
      payloadType: 'query',
      fields: queryFields,
      query,
    }).then((res) => {
      genDownloadLink(res);
    });
  }
).perform;

export const bulkExportByIds = createAsyncActions(
  'bulkExportByIds',
  function(ids, description, fields, entity, identifier) {
    const queryFields = getFields(fields, identifier);
    const payload = {
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
