import _ from 'lodash';
import { assoc } from 'sprout-data';
import { createAction } from 'redux-act';

const isServer = typeof self == 'undefined';

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

function createAsyncAction(namespace, type, payloadReducer) {
  const description = `${namespace.toUpperCase()}_${type.toUpperCase()}`;
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

  // @TODO! think about cancelling request on client side
  // for example in navigate to product 1, then to 2, then back to 1 and there user will see
  // not last navigated product, but product for which response will be last

  const perform = (...args) => {
    return (dispatch, getState, api) => {
      const apiContext = {
        api,
        dispatch,
      };
      const handleError = err => {
        const httpStatus = _.get(err, 'response.status');
        if (httpStatus != 404) {
          console.error(err && err.stack);
        }
        dispatch(failed(err));
        throw err;
      };

      dispatch(started(...args));
      return asyncCall.call(apiContext, ...args)
        .then(
          result => {
            dispatch(succeeded(result));
            return result;
          },
          handleError
        ).catch(handleError);
    };
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
    fetch: lazyPerform, // for backward compartibility
    perform: lazyPerform,
    started,
    succeeded,
    failed,
    clearErrors,
  };
}
