
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
  return (makeUrl, moduleReducer) => {
    return {
      reducer: paginateReducer(dataNamespace, moduleReducer, makeUpdateBehaviour(dataPath)),
      actions: createActions(dataNamespace, dataPath)(makeUrl),
    };
  };
};

export {
  makePagination as default,
  createActions,
  paginateReducer
};
