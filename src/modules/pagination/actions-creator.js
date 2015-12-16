
import Api from '../../lib/api';
import {createFetchActions, pickFetchParams} from './base';

const createActions = (namespace, dataPath, payloadReducer = (identity, payload) => [identity, payload]) => {
  return makeUrl => {
    const fetchActions = createFetchActions(namespace, payloadReducer);
    const {
      actionFetch,
      actionReceived,
      actionFetchFailed,
      actionSetFetchParams
      } = fetchActions;

    const fetch = (identity, newFetchParams) => dispatch => {
      const fetchParams = pickFetchParams(newFetchParams);

      dispatch(actionFetch(identity));
      dispatch(actionSetFetchParams(identity, newFetchParams));
      const url = makeUrl(identity);

      return Api.get(url, fetchParams)
        .then(
          result => dispatch(actionReceived(identity, result)),
          err => dispatch(actionFetchFailed(identity, err))
        );
    };

    return {
      fetch,
      ...fetchActions
    };
  };
};

export default createActions;
