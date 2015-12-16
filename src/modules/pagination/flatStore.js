
import _ from 'lodash';
import {createActions, paginateReducer} from './v2';

const makePagination = dataNamespace => {
  const makeReducer = (reducer, updateBehaviour) => paginateReducer(dataNamespace, reducer, updateBehaviour);
  const makeActions = makeUrl => createActions(dataNamespace, () => [], (identity, payload) => payload)(makeUrl);

  return {
    makeReducer,
    makeActions,
  };
};

export default makePagination;
