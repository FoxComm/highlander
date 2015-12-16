
import _ from 'lodash';
import flatStore from './flat-store';
import flatStoreDynamicUrl from './flat-store-dynamic-url';

export default function(url, namespace, reducer) {
  const makePagination = _.isString(url) ? flatStore : flatStoreDynamicUrl;

  const {makeActions, makeReducer} = makePagination(namespace);

  return {
    reducer: makeReducer(reducer),
    ...makeActions(url),
  };
}
