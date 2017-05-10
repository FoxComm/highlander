// libs
import { flow, filter, getOr, invoke, reduce, set } from 'lodash/fp';

// helpers
import Api from 'lib/api';
import { singularize } from 'fleck';
import createStore from 'lib/store-creator';

// data
import { reducers, createExportByIds } from '../bulk';

const getCustomer = (getState, ids) => {
  return flow(
    invoke('customers.list.currentSearch'),
    getOr([], 'results.rows'),
    filter(c => ids.indexOf(c.id) !== -1),
    reduce((obj, c) => set(c.id, c.name, obj), {})
  )(getState());
};

const addCustomersToGroup = (actions, groupId, customersIds = []) => (dispatch, getState) => {
  dispatch(actions.bulkRequest());

  const customers = getCustomer(getState, customersIds);

  return Api.post(`/customer-groups/${groupId}/customers`, { toAdd: customersIds, toDelete: [], })
    .then(() => dispatch(actions.bulkDone(customers, null)))
    .catch(error => dispatch(actions.bulkError(error)));
};

const exportByIds = createExportByIds(getCustomer);

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
