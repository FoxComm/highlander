/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from 'modules/bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getTransactionIds = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('inventory.transactions', ids, ['id'], getState());
};

const exportByIds = createExportByIds(getTransactionIds);

const { actions, reducer } = createStore({
  path: 'inventory.transactions.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
