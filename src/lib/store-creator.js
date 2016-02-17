// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

// helpers
import { toConstName, capitalize } from '../lib/text-utils';


function getActionDescription(entity, name) {
  if (entity) {
    return toConstName(entity + capitalize(name));
  }

  return toConstName(name);
}

function payloadReducer(...args) {
  if (arguments.length > 1) {
    return [...args];
  }

  return arguments[0];
}

/**
 * Store creator function
 * Accepts actions map, creators map and initial state
 *
 * Actions are separated into plain actions and complex actions
 * 1. plain actions. Are created by createAction. Are used in reducer. Payload reducer is automatic.
 * 2. complex actions. Cannot be used in reducer
 *
 * @param {String}  entity        name of entity, actions are created for
 * @param {Object}  actionsMap    map of complex actions
 * Map format:
 * {
 *   complexAction: (...args, actions) => { dispatch => dispatch(actions.basicAction(...args)) },
 * }
 * Here by, createAction is called as: createAction('PERFORM_ACTION', map.PERFORM_ACTION.payload)
 * @param {Object}  creatorsMap   map of action creators
 * Map format:
 * {
 *   basicAction: (state, payload) => {state},
 * }
 * all plain actions are passed in the last argument to the wrapped complex actions
 * Reducer, respectively, is created with all plain actions
 * @param {Object}  initialState  initial state for reducer, passed as is
 */
export default function createStore(entity, actionsMap, creatorsMap, initialState) {
  const creators = {};
  const reducerActions = {};

  _.each(creatorsMap, (handler, name) => {
    //create action with entity prefix and default creator
    const action = creators[name] = createAction(getActionDescription(entity, name), payloadReducer);

    //add it to reducerActions
    reducerActions[action] = handler;
  });

  const actions = {};

  _.each(actionsMap, (handler, name) => {
    actions[name] = (...args) => handler(...args, creators);
  });

  return {
    actions: {
      ...creators,
      ...actions,
    },
    reducer: createReducer(reducerActions, initialState),
  };
}
