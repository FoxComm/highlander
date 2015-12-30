import _ from 'lodash';
import Api from '../lib/api';
import { searchAdmins } from '../elastic/store-admins';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

export const toggleWatchers = createAction('WATCHERS_TOGGLE', (entity, group) => [entity, group]);
export const showAddingModal = createAction('WATCHERS_ADDING_MODAL_SHOW', (entity, group) => [entity, group]);
export const closeAddingModal = createAction('WATCHERS_ADDING_MODAL_CLOSE');
export const itemSelected = createAction('WATCHERS_SELECT_NEW', (entity, item) => [entity, item]);
export const itemDeleted = createAction('WATCHERS_DELETE_NEW', (entity, name, idx) => [entity, name, idx]);

const setSuggestedWathcers = createAction('WATCHERS_SET_SUGGERSTED_WATCHERS', (entity, payload) => [entity, payload]);
const setWatchers = createAction('WATCHERS_SET_WATCHERS', (entity, payload) => [entity, payload]);
const setAssignees = createAction('WATCHERS_SET_ASSIGNEES', (entity, payload) => [entity, payload]);
const assignWatchers = createAction('WATCHERS_ASSIGN');
const deleteFromGroup = createAction('WATCHERS_DELETE_FROM_GROUP', (entity, group, name) => [entity, group, name]);

export function fetchWatchers(entity) {
  return dispatch => {
    return Api.get('/fakeurl').then(
      () => dispatch(setWatchers(entity, fakeWatchers)),
      () => dispatch(setWatchers(entity, fakeWatchers))
    );
  };
}

export function fetchAssignees(entity) {
  return dispatch => {
    return Api.get('/fakeurl').then(
      () => dispatch(setAssignees(entity, fakeAssignees)),
      () => dispatch(setAssignees(entity, fakeAssignees))
    );
  };
}

export function suggestWatchers(entity, term) {
  return dispatch => {
    return searchAdmins(term).then(
      (data) => {
        const hits = _.get(data, ['hits', 'hits'], []);
        const admins = hits.reduce(function (acc, item) {
          const admin = _.get(item, '_source');
          acc.push(admin);
          return acc;
        }, []);
        return dispatch(setSuggestedWathcers(entity, admins));
      },
      () => dispatch(setSuggestedWathcers(entity, []))
    );
  };
}

export function addWatchers(entity) {
  return (dispatch, getState) => {
    const state = getState();
    const {entityType, entityId} = entity;
    console.log(state);
    const items = _.get(state, ['watchers', entityType, entityId, 'selectedItems'], []);
    console.log(items);
    const group = _.get(state, ['watchers', entityType, entityId, 'modalGroup']);
    console.log(group);

    // Api calls will be here
    dispatch(assignWatchers(entity));
    dispatch(closeAddingModal(entity));
  };
}

export function removeFromGroup(entity, group, name) {
  return dispatch => {
    dispatch(deleteFromGroup(entity, group, name));
  };
}

const initialState = {};

const reducer = createReducer({
  [toggleWatchers]: (state, [{entityType, entityId}, group]) => {
    const oldValue = _.get(state, [entityType, entityId, group, 'displayed'], false);
    return assoc(state, [entityType, entityId, group, 'displayed'], !oldValue);
  },
  [showAddingModal]: (state, [{entityType, entityId}, group]) => {
    return assoc(state,
      [entityType, entityId, 'modalDisplayed'], true,
      [entityType, entityId, 'modalGroup'], group
    );
  },
  [closeAddingModal]: (state, {entityType, entityId}) => {
    return assoc(state,
      [entityType, entityId, 'modalDisplayed'], false,
      [entityType, entityId, 'modalGroup'], null
    );
  },
  [setWatchers]: (state, [{entityType, entityId}, payload]) => {
    return assoc(state, [entityType, entityId, 'watchers', 'entries'], payload);
  },
  [setAssignees]: (state, [{entityType, entityId}, payload]) => {
    return assoc(state, [entityType, entityId, 'assignees', 'entries'], payload);
  },
  [setSuggestedWathcers]: (state, [{entityType, entityId}, payload]) => {
    return assoc(state, [entityType, entityId, 'suggestedWatchers'], payload);
  },
  [itemSelected]: (state, [{entityType, entityId}, item]) => {
    const items = _.get(state, [entityType, entityId, 'selectedItems'], []);
    const newItems = items.concat(item);
    return assoc(state, [entityType, entityId, 'selectedItems'], newItems);
  },
  [itemDeleted]: (state, [{entityType, entityId}, name, idx]) => {
    const group = _.get(state, [entityType, entityId, 'modalGroup']);
    const items = _.get(state, [entityType, entityId, 'selectedItems'], []);
    const newItems = [];
    _.each(items, item => {if (item.name !== name) newItems.push(item);});
    return assoc(state,
      [entityType, entityId, 'selectedItems'], newItems
    );
  },
  [assignWatchers]: (state, {entityType, entityId}) => {
    const items = _.get(state, [entityType, entityId, 'selectedItems'], []);
    const group = _.get(state, [entityType, entityId, 'modalGroup']);
    const groupEntries = _.get(state, [entityType, entityId, group, 'entries'], []);
    const newEntries = groupEntries.concat(items);
    return assoc(state,
      [entityType, entityId, 'modalGroup'], null,
      [entityType, entityId, 'selectedItems'], [],
      [entityType, entityId, group, 'entries'], newEntries
    );
  },
  [deleteFromGroup]: (state, [{entityType, entityId}, group, name]) => {
    const groupEntries = _.get(state, [entityType, entityId, group, 'entries'], []);
    const newItems = [];
    _.each(groupEntries, item => {if (item.name !== name) newItems.push(item);});
    return assoc(state,
      [entityType, entityId, group, 'entries'], newItems
    );
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
