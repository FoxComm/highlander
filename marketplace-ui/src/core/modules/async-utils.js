/* @flow weak */

import _ from 'lodash';
import { assoc } from 'sprout-data';
import { createAction } from 'redux-act';

export type Error = {
  status: number;
  statusText: string;
  messages: string;
}

export type AsyncModuleState = {
  inProgress: boolean;
  finished: boolean;
  isReady: boolean;
  err: ?Error;
}

export type State = {
  [x: string]: AsyncModuleState;
}

const isServer = typeof self == 'undefined';

const initialState: State = {};

export function reducer(state: State = initialState, action) {
  const kind = _.get(action, 'meta.kind');
  const payload = action.payload;

  if (kind == 'async') {
    const { type, namespace } = action.meta;
    switch (type) {
      case 'started':
        return assoc(state,
          [namespace, 'inProgress'], true,
          [namespace, 'finished'], false,
          [namespace, 'err'], null
        );
      case 'succeeded':
        return assoc(state,
          [namespace, 'inProgress'], false,
          [namespace, 'finished'], true,
          [namespace, 'err'], null,
          [namespace, 'isReady'], isServer
        );
      case 'failed': {
        const error = {
          status: _.get(payload, 'response.status'),
          statusText: _.get(payload, 'response.statusText', ''),
          messages: _.get(payload, 'responseJson.errors', []),
        };

        return assoc(state,
          [namespace, 'inProgress'], false,
          [namespace, 'finished'], true,
          [namespace, 'err'], error,
          [namespace, 'isReady'], isServer
        );
      }
      case 'clearErrors':
        return assoc(state,
          [namespace, 'err'], null
        );
      case 'resetReadyFlag':
        return assoc(state,
          [namespace, 'isReady'], null
        );
      default:
        return state;
    }
  }
  return state;
}

type Selector = () => string;

export const getActionState = (state: State) => (selector: Selector) => _.get(state, selector());
export const getActionInProgress = (state: State, namespace) => _.get(state, `${namespace}.inProgress`, false);
export const getActionFailed = (state: State, namespace) => !!_.get(state, `${namespace}.err`, null);

function createAsyncAction(namespace, type, payloadReducer) {
  const description = `${_.snakeCase(namespace).toUpperCase()}_${type.toUpperCase()}`;

  return createAction(description, payloadReducer, () => ({
    kind: 'async',
    namespace,
    type,
  }));
}

export default function createAsyncActions(namespace, asyncCall, payloadReducer) {
  const started = createAsyncAction(namespace, 'started', payloadReducer);
  const succeeded = createAsyncAction(namespace, 'succeeded', payloadReducer);
  const failed = createAsyncAction(namespace, 'failed', payloadReducer);
  const clearErrors = createAsyncAction(namespace, 'сlearErrors', payloadReducer);
  const resetReadyFlag = createAsyncAction(namespace, 'resetReadyFlag', payloadReducer);

  /* eslint-disable consistent-return */

  const perform = (...args) => dispatch => {
    const handleError = err => {
      const httpStatus = _.get(err, 'response.status');
      if (httpStatus != 404) {
        console.error(err && err.stack);
      }
      dispatch(failed(err));
      throw err;
    };

    dispatch(started(...args));

    return asyncCall(...args)
      .then(result => {
        dispatch(succeeded(result));

        return result;
      })
      .catch(handleError);
  };

  const lazyPerform = (...args) => {
    let promise;

    return (dispatch, getState) => {
      const asyncState = _.get(getState(), ['asyncActions', namespace]);
      if (asyncState && isServer && (asyncState.inProgress || asyncState.finished)) {
        return promise;
      }

      // if we already have hydrated state at server we don't need do request for first time
      if (asyncState && !isServer && asyncState.isReady) {
        return dispatch(resetReadyFlag());
      }

      promise = dispatch(perform(...args));
      return promise;
    };
  };

  /* eslint-enable consistent-return */

  return {
    perform: lazyPerform,
    started,
    succeeded,
    failed,
    clearErrors,
  };
}
