
import { update } from 'sprout-data';
import createActions from './actions-creator';
import {paginateReducer, paginate} from './base';

export const makeUpdateBehaviour = dataPath => (state, action, actionType) => {
  // behaviour for initial state
  if (actionType === void 0) return state;

  const [entity, payload] = action.payload;

  return update(state, dataPath(entity), paginate, {
    ...action,
    payload,
    type: actionType
  });
};

const makePagination = (namespace, dataPath) => {
  const makeReducer = moduleReducer => paginateReducer(namespace, moduleReducer, makeUpdateBehaviour(dataPath));
  const makeActions = makeUrl => createActions(namespace, dataPath)(makeUrl);

  return {
    makeReducer,
    makeActions,
  };
};

export default makePagination;
