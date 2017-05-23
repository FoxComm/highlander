/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from '../bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getSkus = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('skus', ids, ['skuCode'], getState());
};

const exportByIds = createExportByIds(getSkus);

const { actions, reducer } = createStore({
  path: 'skus.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
