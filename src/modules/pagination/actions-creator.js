
import Api from '../../lib/api';
import {createFetchActions, pickFetchParams} from './base';

const createActions = (namespace, dataPath, payloadReducer = (entity, payload) => [entity, payload]) => {
  return makeUrl => {
    const fetchActions = createFetchActions(namespace, payloadReducer);
    const {
      actionFetch,
      actionReceived,
      actionFetchFailed,
      actionSetFetchParams
      } = fetchActions;

    const fetch = (entity, newFetchParams) => dispatch => {
      const fetchParams = pickFetchParams(newFetchParams);

      dispatch(actionFetch(entity));
      dispatch(actionSetFetchParams(entity, newFetchParams));
      const url = makeUrl(entity);

      return Api.get(url, fetchParams)
        .then(
          result => dispatch(actionReceived(entity, result)),
          err => dispatch(actionFetchFailed(entity, err))
        );
    };

    return {
      fetch,
      ...fetchActions
    };
  };
};

export default createActions;
