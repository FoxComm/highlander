
import _ from 'lodash';
import createActions from './actions-creator';
import paginateReducer from './base';

const makePagination = namespace => {
  const makeReducer = (reducer, updateBehaviour) => paginateReducer(namespace, reducer, updateBehaviour);
  const makeActions = url => createActions(() => url, namespace);

  return {
    makeReducer,
    makeActions,
  };
};

export default makePagination;
