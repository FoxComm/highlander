/* @flow */

// libs
import _ from 'lodash';
import Api from '../lib/api';
import { pluralize } from 'fleck';

import type { Store} from '../lib/store-creator';
import createStore from '../lib/store-creator';

export const initialState = {
  isFetching: false,
  messages: {},
};

export const reducers = {
  bulkRequest: (state: Object): Object => {
    return {
      ...state,
      isFetching: true,
    };
  },
  bulkDone: (state: Object, [successes, errors]: Array<Object>): Object => {
    return {
      ...state,
      isFetching: false,
      successes: _.isEmpty(successes) ? null : successes,
      errors: _.isEmpty(errors) ? null : errors,
    };
  },
  bulkError: (state: Object, error: Object): Object => {
    return {
      ...state,
      error: error
    };
  },
  clearError: (state: Object): Object => {
    return {
      ...state,
      error: null
    };
  },
  setMessages: (state: Object, messages: Object): Object => {
    return {
      ...state,
      messages,
    };
  },
  reset: (): Object => {
    return {
      ...initialState,
    };
  },
  clearSuccesses: (state: Object): Object => {
    return _.omit(state, 'successes');
  },
  clearErrors: (state: Object): Object => {
    return _.omit(state, 'errors');
  },
};

export function getSuccesses(entityType: string, entityIds: Array<string|number>, bulkStatus: string): Object {
  const bulkFailures = _.get(bulkStatus, `failures.${entityType}`, {});

  return entityIds
    .filter(entityId => !(entityId in bulkFailures))
    .reduce((result, entityId) => {
      return {
        ...result,
        [entityId]: []
      };
    }, {});
}

export function toggleWatch(isDirectAction: bool): Function {
  return (actions: Object, entityType: string, group: string, entityIds: Array<string|number>, watchers: Array<string|number>): Function => {
    const prefix = pluralize(entityType);

    return dispatch => {
      dispatch(actions.bulkRequest());

      const url = isDirectAction ? `/${prefix}/${group}` : `/${prefix}/${group}/delete`;
      // UI modal popup has restriction to have only one watcher
      // but interface of this function is not limited to this behaviour and it's more general than
      // so we take first entry here since ui popup can't produce more anyway
      const storeAdminId = watchers[0];

      Api.post(url, {
        entityIds,
        storeAdminId,
      })
        .then(
          ({batch}) => {
            const errors = _.get(batch, `failures.${entityType}`);
            dispatch(actions.bulkDone(getSuccesses(entityType, entityIds, batch), errors));
          },
          error => {
            dispatch(actions.bulkError(error));
          }
        );
    };
  };
}

export const bulkActions = {
  watch: toggleWatch(true),
  unwatch: toggleWatch(false),
};

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

export default function makeBulkActions(path: string): Store {
  return createStore({
    path,
    actions: {
      changeState,
      updateAttributes,
      ...bulkActions,
    },
    reducers,
    initialState,
  });
}

