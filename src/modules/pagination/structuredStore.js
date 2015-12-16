
import _ from 'lodash';
import { paginate } from './index';
import { update } from 'sprout-data';
import {createActions, paginateReducer} from './v2';

export const makeUpdateBehaviour = dataPath => (state, action, actionType) => {
  // behaviour for initial state
  if (actionType === void 0) return state;

  const [identity, payload] = action.payload;

  return update(state, dataPath(identity), paginate, {
    ...action,
    payload,
    type: actionType
  });
};

const makePagination = (dataNamespace, dataPath) => {
  const makeReducer = moduleReducer => paginateReducer(dataNamespace, moduleReducer, makeUpdateBehaviour(dataPath));
  const makeActions = makeUrl => createActions(dataNamespace, dataPath)(makeUrl);

  return {
    makeReducer,
    makeActions,
  };
};

export default makePagination;
