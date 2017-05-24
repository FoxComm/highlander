/* @flow */

import createStore from 'lib/store-creator';
import { reducers, createExportByIds } from 'modules/bulk';
import { getPropsByIds } from 'modules/bulk-export/helpers';

const getGCTransactions = (getState: Function, ids: Array<number>): Object => {
  return getPropsByIds('giftCards.transactions', ids, ['orderPayment.cordReferenceNumber'], getState());
};

const exportByIds = createExportByIds(getGCTransactions);

const { actions, reducer } = createStore({
  path: 'giftCards.transactions.bulk',
  actions: {
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
