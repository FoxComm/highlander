import _ from 'lodash';
import Api from '../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

export const toggleWatchers = createAction('WATCHERS_TOGGLE', (entity, group) => [entity, group]);
export const showAddingModal = createAction('WATCHERS_ADDING_MODAL_SHOW', (entity, group) => [entity, group]);
export const closeAddingModal = createAction('WATCHERS_ADDING_MODAL_CLOSE', (entity, group) => [entity, group]);

const setSuggestedWathcers = createAction('WATCHERS_SET_SUGGERSTED_WATCHERS', (entity, payload) => [entity, payload]);
const setWatchers = createAction('WATCHERS_SET_WATCHERS', (entity, payload) => [entity, payload]);
const setAssignees = createAction('WATCHERS_SET_ASSIGNEES', (entity, payload) => [entity, payload]);

export function fetchWatchers(entity) {
  return dispatch => {
    return Api.get("/fakeurl").then(
      () => dispatch(setWatchers(entity, fakeWatchers)),
      () => dispatch(setWatchers(entity, fakeWatchers))
    );
  }
}

export function fetchAssignees(entity) {
  return dispatch => {
    return Api.get("/fakeurl").then(
      () => dispatch(setAssignees(entity, fakeAssignees)),
      () => dispatch(setAssignees(entity, fakeAssignees))
    );
  }
}

export function suggestWatchers(entity, term) {
  return dispatch => {
    // API call will be here
    return Api.get('/fakeurl').then(
      () => dispatch(setSuggestedWathcers(entity, fakeData)),
      () => dispatch(setSuggestedWathcers(entity, fakeData))
    );
  };
}

const initialState = {};

const reducer = createReducer({
  [toggleWatchers]: (state, [{entityType, entityId}, group]) => {
    const oldValue = _.get(state, [entityType, entityId, group, 'displayed'], false);
    return assoc(state, [entityType, entityId, group, 'displayed'], !oldValue);
  },
  [showAddingModal]: (state, [{entityType, entityId}, group]) => {
    return assoc(state, [entityType, entityId, 'modalDisplayed'], true);
  },
  [closeAddingModal]: (state, [{entityType, entityId}, group]) => {
    return assoc(state, [entityType, entityId, 'modalDisplayed'], false);
  },
  [setWatchers]: (state, [{entityType, entityId}, payload]) => {
    return assoc(state, [entityType, entityId, 'watchers', 'entries'], payload);
  },
  [setAssignees]: (state, [{entityType, entityId}, payload]) => {
    return assoc(state, [entityType, entityId, 'assignees', 'entries'], payload);
  },
  [setSuggestedWathcers]: (state, [{entityType, entityId}, payload]) => {
    const nextState = assoc(state, [entityType, entityId, 'suggestedWatchers'], payload);
    console.log(nextState);
    return nextState;
  }
}, initialState);

export default reducer;

const fakeData = [
  {name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
  {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
  {name: 'Donkey Sypachev', email: 'eugene@foxcommerce.com'},
  {name: 'Donkey Donkey', email: 'eugene@foxcommerce.com'},
  {name: 'Eugene Donkey', email: 'eugene@foxcommerce.com'}
];

const fakeAssignees = [
  {name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
  {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'}
];

const fakeWatchers = [
  {name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
  {name: 'Donkey Donkey', email: 'eugene@foxcommerce.com'},
  {name: 'Eugene Donkey', email: 'eugene@foxcommerce.com'}
];
