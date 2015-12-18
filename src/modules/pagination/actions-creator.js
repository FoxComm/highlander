
import Api from '../../lib/api';
import {createPaginateActions, pickFetchParams} from './base';

export const createFetchAction = (makeUrl, paginateActions) => {
  const {
    actionFetch,
    actionReceived,
    actionFetchFailed,
    actionSetFetchParams
    } = paginateActions;

  return (...args) => dispatch => {
    const newFetchParams = args.pop();

    const fetchParams = pickFetchParams(newFetchParams);

    dispatch(actionFetch(...args));
    dispatch(actionSetFetchParams(...args, newFetchParams));
    const url = makeUrl(...args);

    return Api.get(url, fetchParams)
      .then(
        result => dispatch(actionReceived(...args, result)),
        err => dispatch(actionFetchFailed(...args, err))
      );
  };
};

const createActions = (makeUrl, namespace, payloadReducer) => {
  const paginateActions = createPaginateActions(namespace, payloadReducer);
  const fetch = createFetchAction(makeUrl, paginateActions);

  return {
    fetch,
    ...paginateActions
  };
};

export default createActions;
