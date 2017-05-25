/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from 'modules/bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getSCTransactions = (getState: Function, ids: Array<number>): Object => {
  return getPropsByIds('customers.storeCreditTransactions', ids, ['orderPayment.cordReferenceNumber'], getState());
};

const exportByIds = createExportByIds(getSCTransactions);

const { actions, reducer } = createStore({
  path: 'customers.storeCreditTransactions.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
