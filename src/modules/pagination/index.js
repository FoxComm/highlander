
import _ from 'lodash';
import createActions from './actions-creator';
import paginateReducer from './base';
import { apiStaticUrl, apiDynamicUrl } from './fetchers';

const makePagination = namespace => {
  const makeReducer = (reducer, updateBehaviour) => paginateReducer(namespace, reducer, updateBehaviour);
  const makeActions = fetcher => createActions(namespace, fetcher);

  return {
    makeReducer,
    makeActions,
  };
};

export default function(url, namespace, reducer) {
  const {makeActions, makeReducer} = makePagination(namespace);

  const fetcher = _.isString(url) ? apiStaticUrl(url) : apiDynamicUrl(url);

  return {
    reducer: makeReducer(reducer),
    ...makeActions(fetcher),
  };
}
