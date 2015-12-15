
import _ from 'lodash';
import Api from '../../lib/api';
import {paginateReducer as originalPaginateReducer, createFetchActions, pickFetchParams} from './index';

const ensureArray = value => _.isArray(value) ? value : [value];

const createNamespace = dataNamespace => ensureArray(dataNamespace).join('_').toUpperCase();

export const paginateReducer = (dataNamespace, ...args) => {
  return originalPaginateReducer(createNamespace(dataNamespace), ...args);
};

export const createActions = (dataNamespace, dataPath, payloadReducer = (identity, payload) => [identity, payload]) => {
  return makeUrl => {
    const fetchActions = createFetchActions(createNamespace(dataNamespace), payloadReducer);
    const {
      actionFetch,
      actionReceived,
      actionFetchFailed,
      actionSetFetchParams
      } = fetchActions;

    const fetch = (identity, newFetchParams) => (dispatch, getState) => {
      const state = _.get(getState(), [...ensureArray(dataNamespace), ...dataPath(identity)]);
      const fetchParams = pickFetchParams(state, newFetchParams);

      dispatch(actionFetch(identity));
      dispatch(actionSetFetchParams(identity));
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
