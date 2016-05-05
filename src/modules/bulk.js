// libs
import _ from 'lodash';
import Api from '../lib/api';
import { pluralize } from 'fleck';

export const initialState = {
  isFetching: false,
  messages: {},
};

export const reducers = {
  bulkRequest: (state) => {
    return {
      ...state,
      isFetching: true,
    };
  },
  bulkDone: (state, [successes, errors]) => {
    return {
      ...state,
      isFetching: false,
      successes: _.isEmpty(successes) ? null : successes,
      errors: _.isEmpty(errors) ? null : errors,
    };
  },
  bulkError: (state, error) => {
    return {
      ...state,
      error: error
    };
  },
  clearError: state => {
    return {
      ...state,
      error: null
    };
  },
  setMessages: (state, messages) => {
    return {
      ...state,
      messages,
    };
  },
  reset: () => {
    return {
      ...initialState,
    };
  },
  clearSuccesses: (state) => {
    return _.omit(state, 'successes');
  },
  clearErrors: (state) => {
    return _.omit(state, 'errors');
  },
};

export function getSuccesses(entityType, entityIds, bulkStatus) {
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

export function toggleWatch(isDirectAction) {
  return (actions, entityType, group, entityIds, watchers) => {
    const prefix = pluralize(entityType);

    return dispatch => {
      dispatch(actions.bulkRequest());

      const url = isDirectAction ? `/${prefix}/${group}` : `/${prefix}/${group}/delete`;
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

