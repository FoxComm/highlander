import _ from 'lodash';
import { assoc } from 'sprout-data';
import { createAction } from 'redux-act';

const registeredActions = Object.create(null);

export function reducer(state = {}, action) {
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
          [namespace, 'err'], null
        );
      case 'failed':
        return assoc(state,
          [namespace, 'inProgress'], false,
          [namespace, 'finished'], true,
          [namespace, 'err'], payload
        );
      case 'aborted':
        return assoc(state,
          [namespace, 'inProgress'], false,
          [namespace, 'finished'], false,
          [namespace, 'err'], null
        );
      case 'clearErrors':
        return assoc(state,
          [namespace, 'err'], null
        );
      default:
        return state;
    }
  }
  return state;
}

function createAsyncAction(namespace, type, payloadReducer) {
  const description = `${_.snakeCase(namespace).toUpperCase()}_${type.toUpperCase()}`;

  return createAction(description, payloadReducer, () => ({
    kind: 'async',
    namespace,
    type,
  }));
}

export default function createAsyncActions(namespace, asyncMethod, payloadReducer) {
  if (namespace in registeredActions) {
    throw new Error(`You already have ${namespace} action`);
  }
  registeredActions[namespace] = true;

  const started = createAsyncAction(namespace, 'started', payloadReducer);
  const succeeded = createAsyncAction(namespace, 'succeeded', payloadReducer);
  const failed = createAsyncAction(namespace, 'failed', payloadReducer);
  const aborted = createAsyncAction(namespace, 'aborted', payloadReducer);
  const clearErrors = createAsyncAction(namespace, 'clearErrors', payloadReducer);

  const perform = (...args) => {
    return (dispatch, getState) => {
      const handleError = err => {
        const httpStatus = _.get(err, 'response.status');
        if (httpStatus != 404) {
          console.error(err && err.stack);
        }
        dispatch(failed(err, ...args));
        throw err;
      };

      const callContext = {
        dispatch,
        getState,
      };

      dispatch(started(...args));
      const promise = asyncMethod.call(callContext, ...args);
      const abort = () => {
        if (promise.abort) promise.abort();
        dispatch(aborted());
      };

      let result = promise
        .then(
          res => dispatch(succeeded(res, ...args)),
          handleError
        );

      // caching errors hinder debugging process
      // but in production it leads to more expected behaviour
      if (process.env.NODE_ENV === 'production') {
        result = result.catch(handleError);
      }
      result.abort = abort;
      return result;
    };
  };

  return {
    perform,
    started,
    succeeded,
    failed,
    clearErrors,
  };
}
