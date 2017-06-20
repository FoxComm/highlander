/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from 'modules/bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getProducts = (getState: Function, ids: Array<number>) => {
  return getPropsByIds('taxons.details', ids, ['id', 'context'], getState(), 'products');
};

const exportByIds = createExportByIds(getProducts);

const { actions, reducer } = createStore({
  path: 'taxons.details.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
