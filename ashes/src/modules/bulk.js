/* @flow */

// libs
import _ from 'lodash';
import Api from '../lib/api';
import { pluralize } from 'fleck';

import type { Store} from '../lib/store-creator';
import createStore from '../lib/store-creator';

import { bulkExportByIds } from 'modules/bulk-export/bulk-export';

export const initialState = {
  isFetching: false,
  messages: {},
};

export const reducers = {
  bulkRequest: function (state: Object): Object {
    return {
      ...state,
      isFetching: true,
    };
  },
  bulkDone: function (state: Object, [successes, errors]: Array<Object>): Object {
    return {
      ...state,
      isFetching: false,
      successes: _.isEmpty(successes) ? null : successes,
      errors: _.isEmpty(errors) ? null : errors,
    };
  },
  bulkError: function (state: Object, error: Object): Object {
    return {
      ...state,
      error: error
    };
  },
  clearError: function (state: Object): Object {
    return {
      ...state,
      error: null
    };
  },
  setMessages: function (state: Object, messages: Object): Object {
    return {
      ...state,
      messages,
    };
  },
  reset: function(): Object {
    return {
      ...initialState,
    };
  },
  clearSuccesses: function(state: Object): Object {
    return _.omit(state, 'successes');
  },
  clearErrors: function (state: Object): Object {
    return _.omit(state, 'errors');
  },
};

type Ids = Array<string|number>;

export function getSuccesses(entityType: string, entityIds: Ids, bulkStatus: string): Object {
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
  return function (actions: Object, entityType: string, group: string, entityIds: Ids, watchers: Ids): Function {
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

function deleteEntity(actions, ids, entityType, onDelete): Function {
  const prefix = pluralize(entityType);
  return dispatch => {
    dispatch(actions.bulkRequest());
    for(let i=0; i<ids.length; i++){
      let id = ids[i];
      let url = `/${prefix}/default/${id}`;
      Api.delete(url)
        .then(
          ({batch}) => {
            onDelete();
            const errors = _.get(batch, `failures.${entityType}`);
            dispatch(actions.bulkDone(getSuccesses(entityType, ids, batch), errors));
          },
          error => {
            dispatch(actions.bulkError(error));
          }
        );
    }
  };
}

export const bulkActions = {
  watch: toggleWatch(true),
  unwatch: toggleWatch(false),
};

function changeState(actions, ids, isActive) {
  return dispatch => {
    const successes = {
      'id1': [],
    };
    const failures = {};
    dispatch(actions.bulkDone(successes, failures));
  };
}

function updateAttributes(actions, ids, object) {
  return dispatch => {
    const successes = {
      'id1': [],
    };
    const failures = {};
    dispatch(actions.bulkDone(successes, failures));
  };
}

export const createExportByIds = (getEntities: (getState: Function, ids: Array<number>) => any) => {
  return (
    actions: Object,
    ids: Array<number>,
    description: string,
    fields: Array<string>,
    entity: string,
    identifier: string
  ) => (dispatch: Function, getState: Function) => {
    dispatch(actions.bulkRequest());

    dispatch(bulkExportByIds(ids, description, fields, entity, identifier))
      .then(() => dispatch(actions.bulkDone(getEntities(getState, ids), null)))
      .catch(err => dispatch(actions.bulkError(err)));
  };
};

export default function makeBulkActions(path: string, extraActions: Object): Store {
  return createStore({
    path,
    actions: {
      changeState,
      updateAttributes,
      deleteEntity,
      ...bulkActions,
      ...extraActions,
    },
    reducers,
    initialState,
  });
}
