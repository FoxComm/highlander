import _ from 'lodash';
import Api from '../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

export const toggleAssignees = createAction('ASSIGNEES_TOGGLE');
export const toggleWatchers = createAction('WATCHERS_TOGGLE');

const initialState = {};

const reducer = createReducer({
  [toggleAssignees]: (state, {entityType, entityId}) => {
    const oldValue = _.get(state, [entityType, entityId, 'assigneesDisplayed'], false);
    return assoc(state, [entityType, entityId, 'assigneesDisplayed'], !oldValue);
  },
  [toggleWatchers]: (state, {entityType, entityId}) => {
    const oldValue = _.get(state, [entityType, entityId, 'watchersDisplayed'], false);
    return assoc(state, [entityType, entityId, 'watchersDisplayed'], !oldValue);
  },
}, initialState);

export default reducer;
