// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';


//created stores storage
const STORES = {};

function saveStore(entity, scope, store) {
  if (getStore(entity, scope)) {
    throw new TypeError(`Store ${entity}:${scope} already exists`);
  }

  _.set(STORES, [entity, scope], store);

  return store;
}

export function getStore(entity, scope) {
  return _.get(STORES, [entity, scope], null);
}


function getActionDescription(entity, scope, name) {
  return _.snakeCase(`${entity}_${scope}_${name}`).toUpperCase();
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
 * @param {String}  entity          name of entity, actions are created for
 * @param {String}  [scope]         scope of created store (allows to use several stores of one entity)
 * @param {Object}  actions         map of complex actions
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
export default function createStore({entity, scope = '', actions, reducers, initialState = {}}) {
  const simpleActions = {};
  const reducersMap = {};

  _.each(reducers, (handler, name) => {
    //create action with entity prefix and default creator
    const simpleAction = simpleActions[name] = createAction(getActionDescription(entity, scope, name), payloadReducer);

    //add it to reducersMap
    reducersMap[simpleAction] = handler;
  });

  const asyncActions = {};

  _.each(actions, (handler, name) => {
    asyncActions[name] = (...args) => handler(simpleActions, ...args);
  });

  return saveStore(entity, scope, {
    actions: {
      ...simpleActions,
      ...asyncActions,
    },
    reducer: createReducer(reducersMap, initialState),
  });
}
