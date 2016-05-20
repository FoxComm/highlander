
import createStore from '../lib/store-creator';

import { initialState, reducers, bulkActions } from './bulk';

export default function makeBulkActions(entity) {
  const changeState = (actions, ids, isActive) => {
    return dispatch => {
      const successes = {
        'id1': [],
      };
      const failures = {};
      dispatch(actions.bulkDone(successes, failures));
    };
  };

  const updateAttributes = (actions, ids, form, shadow) => {
    return dispatch => {
      const successes = {
        'id1': [],
      };
      const failures = {};
      dispatch(actions.bulkDone(successes, failures));
    };
  };

  return createStore({
    entity: 'bulk',
    scope: `${entity}s`,
    actions: {
      changeState,
      updateAttributes,
      ...bulkActions,
    },
    reducers,
    initialState,
  });
}
