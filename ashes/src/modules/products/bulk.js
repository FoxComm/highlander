/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from '../bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getGroups = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('products', ids, ['id', 'context'], getState());
};

const exportByIds = createExportByIds(getGroups);

const { actions, reducer } = createStore({
  path: 'products.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
