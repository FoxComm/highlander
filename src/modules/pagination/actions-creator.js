
import _ from 'lodash';
import { createPaginateActions } from './base';

export const createFetchAction = (namespace, doFetch, paginateActions) => {
  const {
    actionFetch,
    actionReceived,
    actionFetchFailed,
    } = paginateActions;

  return (...args) => (dispatch, getState) => {
    const state = _.get(getState(), namespace);

    dispatch(actionFetch());

    return doFetch(...args, state)
      .then(
        result => dispatch(actionReceived(result)),
        err => dispatch(actionFetchFailed(err))
      );
  };
};

const createActions = (namespace, doFetch, payloadReducer) => {
  const paginateActions = createPaginateActions(namespace, payloadReducer);
  const fetch = createFetchAction(doFetch, paginateActions);

  const { actionUpdateState } = paginateActions;
  const setFetchParams = (newState, ...args) => {
    return dispatch => {
      dispatch(actionUpdateState(newState));
      dispatch(fetch(...args));
    };
  };

  return {
    fetch,
    setFetchParams,
    ...paginateActions
  };
};

export default createActions;
