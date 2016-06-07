// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';


//created stores storage
const STORES = {};

function saveStore(path, store) {
  if (getStore(path)) {
    throw new TypeError(`Store ${entity}:${scope} already exists`);
  }

  _.set(STORES, path, store);

  return store;
}

export function getStore(path) {
  return _.get(STORES, path, null);
}


function getActionDescription(path, name) {
  return _.snakeCase(`${path.join(' ')} ${name}`).toUpperCase();
}

function payloadReducer(...args) {
  if (args.length > 1) {
    return [...args];
  }

  return args[0];
}

/**
 * Store creator function
 * Accepts actions map, creators map and initial state
 *
 * Actions are separated into plain actions and complex actions
 * 1. plain actions. Are created by createAction. Are used in reducer. Payload reducer is automatic.
 * 2. complex actions. Cannot be used in reducer
 *
 * @param {String|String[]}   path            path of created store in global store
 * @param {Object}            actions         map of complex actions
 * Map format:
 * {
 *   complexAction: (...args, actions) => { dispatch => dispatch(actions.basicAction(...args)) },
 * }
 * Here by, createAction is called as: createAction('PERFORM_ACTION', map.PERFORM_ACTION.payload)
 * @param {Object}  reducers        map of action creators
 * Map format:
 * {
 *   basicAction: (state, payload) => {state},
 * }
 * all plain actions are passed in the last argument to the wrapped complex actions
 * Reducer, respectively, is created with all plain actions
 * @param {Object}  [initialState]  initial state for reducer, passed as is
 */
export default function createStore({path, actions, reducers, initialState = {}}) {
  if (_.isArray(path)) {
    path = path.reduce((memo, value) => [...memo, ...value.split('.')], []);
  } else {
    path = path.split('.');
  }

  const allActions = {};
  const reducersMap = {};

  _.each(reducers, (handler, name) => {
    //create action with entity prefix and default creator
    const action = allActions[name] = createAction(getActionDescription(path, name), payloadReducer);

    //add it to reducersMap
    reducersMap[action] = handler;
  });

  _.each(actions, (handler, name) => {
    allActions[name] = (...args) => handler(allActions, ...args);
  });

  return saveStore(path, {
    actions: allActions,
    reducer: createReducer(reducersMap, initialState),
  });
}
