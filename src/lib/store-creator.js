/* @flow */

// libs
import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';

// helpers
import createAsyncAction, { initialState as asyncInitialState } from './async-action-creator';

// types
import type { Dictionary } from '../paragons/types';


// type declarations
export type Store = {
  actions: Dictionary<Function>;
  reducer: Function;
};

type CreatorConfiguration = {
  path: any;
  asyncActions?: Dictionary<Function>;
  actions?: Dictionary<Function>;
  reducers: Dictionary<Function>;
  initialState?: Object;
};


//created stores storage
const STORES : Dictionary<Store> = {};

function saveStore(path: Array<string>, store: Store): Object {
  const stringPath = preparePath(path).join('.');

  if (STORES[stringPath]) {
    throw new TypeError(`Store ${stringPath} already exists`);
  }

  STORES[stringPath] = store;

  return store;
}

export function getStore(path: Array<string>|string): Object {
  const stringPath = preparePath(path).join('.');

  return STORES[stringPath] || null;
}


function getActionDescription(path: Array<string>, name: string): string {
  return _.snakeCase(`${path.join(' ')} ${name}`).toUpperCase();
}

function payloadReducer(...args: Array<any>): Array<any> {
  if (args.length > 1) {
    return [...args];
  }

  return args[0];
}

function restoreArguments(payload: any): Array<any> {
  if (_.isUndefined(payload)) {
    return [];
  }

  if (_.isArray(payload)) {
    return payload;
  }

  return [payload];
}

function preparePath(path: string|Array<string>): Array<string> {
  if (Array.isArray(path)) {
    path = path.reduce((memo, value) => [...memo, ...value.split('.')], []);
  } else if (typeof path === 'string') {
    path = path.split('.');
  }

  return _.compact(path);
}

function registerReducer(path: Array<string>, actions: Dictionary<Function>, reducers: Dictionary<Function>): Function {
  return function (handler: Function, name: string): void {
    //create action with entity prefix and default creator
    const action = actions[name] = createAction(getActionDescription(path, name), payloadReducer);

    //add it to reducersMap
    reducers[action] = handler;
  };
}

function registerAction(actions: Dictionary<Function>): Function {
  return function (handler: Function, name: string): void {
    actions[name] = (...args) => handler(actions, ...args);
  };
}

function registerAsyncAction(path: Array<string>, actions: Dictionary<Function>, reducers: Dictionary<Function>, initialState: Object): Function {
  return function (handler: Function, name: string): void {
    //state of async action is stored under it's name in given path
    const asyncStateStorePath = _.compact([...path, name]).join('.');
    const asyncAction = createAsyncAction(asyncStateStorePath, handler);

    //add async action reducers, bridging requests to asyncAction.reducer
    _.each(asyncAction.actions, action => {
      //here meta is lost, cause it's impossible to restore original arguments from reduced payload and meta
      //arguments are restored from payload as it's
      reducers[action] = (state, payload) => ({
        ...state,
        [name]: asyncAction.reducer(state[name], action.raw(...restoreArguments(payload))),
      });
    });

    //add asyncInitialState to given initial state
    initialState[name] = asyncInitialState;

    //actions are passed on call to be late binded. Otherwise, `actions` map would be incomplete
    actions[name] = (...args) => asyncAction({...actions, ...asyncAction.actions}, ...args);
  };
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
 * @param {Object}            asyncActions    map of async actions
 * Map format:
 * {
 *   asyncAction: (actions, asyncState, ...args) => { dispatch => dispatch(actions.basicAction(...args)) },
 * }
 * actions along with other complex actions contain local actions for managing async operation:
 * - reset,
 * - canceled,
 * - started,
 * - updated,
 * - succeeded
 * - failed
 * asyncState is a local state of async operation.
 * As far as it can be changed externally,
 * @param {Object}            actions         map of complex actions
 * Map format:
 * {
 *   complexAction: (actions, ...args) => { dispatch => dispatch(actions.basicAction(...args)) },
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
export default function createStore({path, asyncActions, actions, reducers, initialState}: CreatorConfiguration): Store {
  path = preparePath(path);

  //restoring Object type from ?Object
  initialState = initialState == null ? {} : initialState;

  const actionsMap = {};
  const reducersMap = {};

  //register reducers
  _.each(reducers, registerReducer(path, actionsMap, reducersMap));

  //register actions
  _.each(actions, registerAction(actionsMap));

  //register async actions
  _.each(asyncActions, registerAsyncAction(path, actionsMap, reducersMap, initialState));

  return saveStore(path, {
    actions: actionsMap,
    reducer: createReducer(reducersMap, initialState),
  });
}
