// libs
import { flow, filter, getOr, invoke, reduce, set } from 'lodash/fp';

// helpers
import Api from 'lib/api';
import { singularize } from 'fleck';
import createStore from 'lib/store-creator';

// data
import { reducers } from '../../bulk';


const deleteCustomersFromGroup = (actions, groupId, customersIds) => (dispatch, getState) => {
  dispatch(actions.bulkRequest());

  const customers = flow(
    invoke('customerGroups.details.customers.currentSearch'),
    getOr([], 'results.rows'),
    filter(c => customersIds.indexOf(c.id) > -1),
    reduce((obj, c) => set(c.id, c.name, obj), {})
  )(getState());

  return Api.post(`/customer-groups/${groupId}/customers`, { toAdd: [], toDelete: customersIds, })
    .then(() => dispatch(actions.bulkDone(customers, null)))
    .catch(error => dispatch(actions.bulkError(error)));
};

const { actions, reducer } = createStore({
  path: 'customerGroups.details.bulk',
  actions: {
    deleteCustomersFromGroup,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
