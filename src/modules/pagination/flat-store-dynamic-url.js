
import createActions from './actions-creator';
import paginateReducer from './base';

const makePagination = namespace => {
  const makeReducer = (reducer, updateBehaviour) => paginateReducer(namespace, reducer, updateBehaviour);
  const makeActions = makeUrl => createActions(namespace, () => [], (identity, payload) => payload)(makeUrl);

  return {
    makeReducer,
    makeActions,
  };
};

export default makePagination;
