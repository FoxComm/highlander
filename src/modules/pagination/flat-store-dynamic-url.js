
import createActions from './actions-creator';
import paginateReducer from './base';

const makePagination = namespace => {
  const makeReducer = (reducer, updateBehaviour) => paginateReducer(namespace, reducer, updateBehaviour);
  const makeActions = makeUrl => createActions(makeUrl, namespace);

  return {
    makeReducer,
    makeActions,
  };
};

export default makePagination;
