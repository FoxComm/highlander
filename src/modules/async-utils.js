import _ from 'lodash';
import { assoc } from 'sprout-data';
import { createAction } from 'redux-act';

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

export default function createAsyncActions(namespace, asyncCall, payloadReducer) {
  const started = createAsyncAction(namespace, 'started', payloadReducer);
  const succeeded = createAsyncAction(namespace, 'succeeded', payloadReducer);
  const failed = createAsyncAction(namespace, 'failed', payloadReducer);
  const clearErrors = createAsyncAction(namespace, 'clearErrors', payloadReducer);

  // @TODO! think about cancelling request on client side
  // for example in navigate to product 1, then to 2, then back to 1 and there user will see
  // not last navigated product, but product for which response will be last

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
      return asyncCall.call(callContext, ...args)
        .then(
          res => dispatch(succeeded(res, ...args)),
          handleError
        ).catch(handleError);
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
