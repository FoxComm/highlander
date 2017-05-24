/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from 'modules/bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getCustomerOrders = (getState: Function, ids: Array<number>): Object => {
  return getPropsByIds('customers.transactions', ids, ['referenceNumber'], getState());
};

const exportByIds = createExportByIds(getCustomerOrders);

const { actions, reducer } = createStore({
  path: 'customers.transactions.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
