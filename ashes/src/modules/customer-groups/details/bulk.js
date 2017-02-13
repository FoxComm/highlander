// libs
import { get, partial } from 'lodash';

// helpers
import Api from 'lib/api';
import { singularize } from 'fleck';
import createStore from 'lib/store-creator';

// data
import { reducers, getSuccesses as _getSuccesses, bulkActions } from '../../bulk';

const getSuccesses = partial(_getSuccesses, 'customerGroup');

const deleteCustomersFromGroup = (actions, groupId, customersIds) => dispatch => {
  dispatch(actions.bulkRequest());
  Api.post(`/customer-groups/${groupId}/customers`, { toAdd: [], toDelete: customersIds, })
    .then(({ batch }) => {
      const errors = get(batch, 'failures.order');
      dispatch(actions.bulkDone(getSuccesses(referenceNumbers, batch), errors));
    })
    .catch(error => dispatch(actions.bulkError(error)));
};

const { actions, reducer } = createStore({
  path: 'customerGroups.bulk',
  actions: {
    deleteCustomersFromGroup,
    ...bulkActions,
  },
  reducers,
});

export {
  actions,
  reducer as default
};
