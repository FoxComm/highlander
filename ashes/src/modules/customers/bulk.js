/* @flow */

// helpers
import Api from 'lib/api';
import createStore from 'lib/store-creator';
import { getPropsByIds } from 'modules/bulk-export/helpers';

// data
import { reducers, createExportByIds } from '../bulk';

const getCustomers = (getState: Function, ids: Array<number>): Object => {
  return getPropsByIds('customers', ids, ['id', 'name'], getState());
};

const addCustomersToGroup = (actions: Object, groupId: number, customersIds: Array<number> = []) =>
  (dispatch: Function, getState: Function) => {
    dispatch(actions.bulkRequest());

    const customers = getCustomers(getState, customersIds);

    return Api.post(`/customer-groups/${groupId}/customers`, { toAdd: customersIds, toDelete: [], })
      .then(() => dispatch(actions.bulkDone(customers, null)))
      .catch(error => dispatch(actions.bulkError(error)));
  };

const exportByIds = createExportByIds(getCustomers);

const { actions, reducer } = createStore({
  path: 'customers.bulk',
  actions: {
    addCustomersToGroup,
    exportByIds,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
