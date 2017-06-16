/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from '../bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getSkus = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('inventory', ids, ['sku'], getState());
};

const exportByIds = createExportByIds(getSkus);

const { actions, reducer } = createStore({
  path: 'inventory.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
