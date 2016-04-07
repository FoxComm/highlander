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
        const error = {
          status: _.get(payload, 'response.status'),
          statusText: _.get(payload, 'response.statusText', ''),
          messages: _.get(payload, 'responseJson.error', []),
        };

        return assoc(state,
          [namespace, 'inProgress'], false,
          [namespace, 'finished'], true,
          [namespace, 'err'], error
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

  // @TODO! think about cancelling request on client side
  // for example in navigate to product 1, then to 2, then back to 1 and there user will see
  // not last navigated product, but product for which response will be last

  const perform = (...args) => {
    return dispatch => {
      dispatch(started(...args));
      return asyncCall(...args)
        .then(
          res => dispatch(succeeded(res))
        ).catch(err => {
          console.error(err && err.stack);
          dispatch(failed(err));
        });
    };
  };

  return {
    perform,
    started,
    succeeded,
    failed,
  };
}
