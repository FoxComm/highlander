import _ from 'lodash';
import Api from '../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

export const toggleWatchers = createAction('WATCHERS_TOGGLE', (entity, group) => [entity, group]);
export const showAddingModal = createAction('WATCHERS_ADDING_MODAL_SHOW', (entity, group) => [entity, group]);
export const closeAddingModal = createAction('WATCHERS_ADDING_MODAL_CLOSE', (entity, group) => [entity, group]);

const setSuggestedWathcers = createAction('WATCHERS_SET_SUGGERSTED_WATCHERS', (entity, payload) => [entity, payload]);

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
  [setSuggestedWathcers]: (state, [{entityType, entityId}, payload]) => {
    console.log("setSuggestedWathcers");
    console.log(payload);
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
