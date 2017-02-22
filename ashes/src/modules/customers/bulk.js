// libs
import { flow, filter, getOr, invoke, reduce, set } from 'lodash/fp';

// helpers
import Api from 'lib/api';
import { singularize } from 'fleck';
import createStore from 'lib/store-creator';

// data
import { reducers } from '../bulk';

const addCustomersToGroup = (actions, groupId, customersIds = []) => (dispatch, getState) => {
  dispatch(actions.bulkRequest());

  const customers = flow(
    invoke('customers.list.currentSearch'),
    getOr([], 'results.rows'),
    filter(c => customersIds.indexOf(c.id) > -1),
    reduce((obj, c) => set(c.id, c.name, obj), {})
  )(getState());

  return Api.post(`/customer-groups/${groupId}/customers`, { toAdd: customersIds, toDelete: [], })
    .then(() => dispatch(actions.bulkDone(customers, null)))
    .catch(error => dispatch(actions.bulkError(error)));
};

const { actions, reducer } = createStore({
  path: 'customers.bulk',
  actions: {
    addCustomersToGroup,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
