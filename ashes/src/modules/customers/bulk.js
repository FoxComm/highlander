// libs
import { get, partial } from 'lodash';

// helpers
import Api from 'lib/api';
import { singularize } from 'fleck';
import createStore from 'lib/store-creator';

// data
import { reducers, bulkActions } from '../bulk';

const addCustomersToGroup = (actions, groupId, customersIds) => dispatch => {
  dispatch(actions.bulkRequest());

  return Api.post(`/customer-groups/${groupId}/customers`, { toAdd: customersIds, toDelete: [], })
    .then(() => dispatch(actions.bulkDone(customersIds, null)))
    .catch(error => dispatch(actions.bulkError(error)));
};

const { actions, reducer } = createStore({
  path: 'customers.bulk',
  actions: {
    addCustomersToGroup,
    ...bulkActions,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
