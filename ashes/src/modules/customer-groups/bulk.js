/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from '../bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getGroups = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('customerGroups', ids, ['id', 'name'], getState());
};

const exportByIds = createExportByIds(getGroups);

const { actions, reducer } = createStore({
  path: 'customerGroups.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
