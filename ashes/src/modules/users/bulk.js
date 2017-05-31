/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from '../bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getTaxons = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('users', ids, ['id', 'name'], getState());
};

const exportByIds = createExportByIds(getTaxons);

const { actions, reducer } = createStore({
  path: 'users.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
