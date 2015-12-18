
import Api from '../../lib/api';
import {createPaginateActions, pickFetchParams} from './base';

export const createFetchAction = (makeUrl, paginateActions) => {
  const {
    actionFetch,
    actionReceived,
    actionFetchFailed,
    actionSetFetchParams
    } = paginateActions;

  return (entity, newFetchParams) => dispatch => {
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
};

const createActions = (makeUrl, namespace, payloadReducer = (entity, payload) => [entity, payload]) => {
  const paginateActions = createPaginateActions(namespace, payloadReducer);
  const fetch = createFetchAction(makeUrl, paginateActions);

  return {
    fetch,
    ...paginateActions
  };
};

export default createActions;
