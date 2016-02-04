
import _ from 'lodash';
import makePagination from './base';
import reduceReducers from 'reduce-reducers';
import { apiStaticUrl, apiDynamicUrl } from './fetchers';

export default function(url, namespace, moduleReducer = _.identity) {
  const fetcher = _.isString(url) ? apiStaticUrl(url) : apiDynamicUrl(url);

  const {reducer, ...rest} = makePagination(namespace, fetcher);

  return {
    reducer: reduceReducers(reducer, moduleReducer),
    ...rest
  };
}
