
import Api from '../../lib/api';
import {createPaginateActions, pickFetchParams} from './base';

export const createFetchAction = (makeUrl, paginateActions, actionsArgsReducer = () => []) => {
  const {
    actionFetch,
    actionReceived,
    actionFetchFailed,
    actionSetFetchParams
    } = paginateActions;

  return (...args) => dispatch => {
    const newFetchParams = args.pop();

    const fetchParams = pickFetchParams(newFetchParams);
    const actionsArgs = actionsArgsReducer(args);

    dispatch(actionFetch(...actionsArgs));
    dispatch(actionSetFetchParams(...actionsArgs, newFetchParams));
    const url = makeUrl(...args);

    return Api.get(url, fetchParams)
      .then(
        result => dispatch(actionReceived(...actionsArgs, result)),
        err => dispatch(actionFetchFailed(...actionsArgs, err))
      );
  };
};

const createActions = (makeUrl, namespace, payloadReducer, actionsArgsReducer) => {
  const paginateActions = createPaginateActions(namespace, payloadReducer);
  const fetch = createFetchAction(makeUrl, paginateActions, actionsArgsReducer);
  // fetch with default params
  const initialFetch = (...args) => fetch(...args, {});

  return {
    fetch,
    initialFetch,
    ...paginateActions
  };
};

export default createActions;
