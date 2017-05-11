/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from '../bulk';

const getCarts = (getState, ids) => ids;

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
