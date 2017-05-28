/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from '../bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getCatalogs = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('catalogs', ids, ['id'], getState());
};

const exportByIds = createExportByIds(getCatalogs);

const { actions, reducer } = createStore({
  path: 'catalogs.bulk',
  actions: { exportByIds },
  reducers,
});

export { actions, reducer as default };
