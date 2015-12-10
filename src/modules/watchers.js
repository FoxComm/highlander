import _ from 'lodash';
import Api from '../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

export const toggleWatchers = createAction('WATCHERS_TOGGLE', (entity, group) => [entity, group]);
export const showAddingModal = createAction('ADDING_MODAL_SHOW', (entity, group) => [entity, group]);

const initialState = {};

const reducer = createReducer({
  [toggleWatchers]: (state, [{entityType, entityId}, group]) => {
    const oldValue = _.get(state, [entityType, entityId, group, 'displayed'], false);
    return assoc(state, [entityType, entityId, group, 'displayed'], !oldValue);
  },
  [showAddingModal]: (state, [{entityType, entityId}, group]) => {
    return assoc(state, [entityType, entityId, 'modalDisplayed'], true);
  }
}, initialState);

export default reducer;
