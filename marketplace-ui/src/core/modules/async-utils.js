/* @flow weak */

import { get, snakeCase } from 'lodash';
import { assoc } from 'sprout-data';
import { createAction } from 'redux-act';
import { SubmissionError } from 'redux-form';
import isServer from '../../utils/isServer';

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

const initialState: State = {};

export function reducer(state: State = initialState, action) {
  const kind = get(action, 'meta.kind');
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
          status: get(payload, 'response.status'),
          statusText: get(payload, 'response.statusText', ''),
          message: get(payload, 'message', 'Error'),
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

/** Selectors */
const asyncStateSelector = (requestState: string) => (state: State, namespace: string): AsyncModuleState =>
  get(state, `${namespace}.${requestState}`, false);

export const inProgressSelector = asyncStateSelector('inProgress');
export const finishedSelector = asyncStateSelector('finished');
export const failedSelector = asyncStateSelector('err');

export const fetchedSelector = (...args) => !inProgressSelector(...args) && finishedSelector(...args);
export const succeededSelector = (...args) => finishedSelector(...args) && !failedSelector(...args);

function createAsyncAction(namespace, type, payloadReducer) {
  const description = `${snakeCase(namespace).toUpperCase()}_${type.toUpperCase()}`;

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
    dispatch(started(...args));

    return asyncCall(...args)
      .then(result => {
        dispatch(succeeded(result));

        return result;
      })
      .catch(err => {
        dispatch(failed(err));

        // FIX THIS!!! Used for redux-form server-side validation handling
        if (err instanceof SubmissionError) {
          throw err;
        }
      });
  };

  const lazyPerform = (...args) => {
    let promise;

    return (dispatch, getState) => {
      const asyncState = get(getState(), ['asyncActions', namespace]);
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
