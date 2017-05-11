/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from '../bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getCarts = (getState, ids) => {
  return getPropsByIds('carts', ids, ['referenceNumber'], getState());
};

const exportByIds = createExportByIds(getCarts);

const { actions, reducer } = createStore({
  path: 'carts.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
