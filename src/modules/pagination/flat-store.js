
import _ from 'lodash';
import createActions from './actions-creator';
import paginateReducer from './base';

const makePagination = namespace => {
  const makeReducer = (reducer, updateBehaviour) => paginateReducer(namespace, reducer, updateBehaviour);
  const makeActions = url => {
    const actions = createActions(namespace, () => [], (identity, payload) => payload)(() => url);
    actions.fetch = _.partial(actions.fetch, null);

    return actions;
  };

  return {
    makeReducer,
    makeActions,
  };
};

export default makePagination;
