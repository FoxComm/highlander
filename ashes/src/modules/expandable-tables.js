
import Api from '../lib/api';
import { get, assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

export const toggleDrawerState = createAction('EXPANDABLE_TABLES_TOGGLE_DRAWER', (entity, id) => [entity, id]);

const initialState = {};

const reducer = createReducer({
  [toggleDrawerState]: (state, [{entityType, entityId}, id]) => {
    const currentState = get(state, [entityType, entityId, id], false);
    return assoc(state, [entityType, entityId, id], !currentState);
  }
}, initialState);

export default reducer;
