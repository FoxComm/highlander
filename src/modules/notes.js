import _ from 'lodash';
import Api from '../lib/api';
import reduceReducers from 'reduce-reducers';
import { createAction, createReducer } from 'redux-act';
import { assoc, dissoc, update, get } from 'sprout-data';
import { updateItems } from './state-helpers';
import makeLiveSearch from './live-search';
import processQuery from '../elastic/notes';

const {reducer, actions} = makeLiveSearch(
  'notes.list',
  [],
  'notes_search_view/_search',
  null, {
    processQuery: (query, {getState}) => {
      const currentEntity = _.get(getState(), 'notes.currentEntity');

      return processQuery(currentEntity, query);
    },
    skipInitialFetch: true
  }
);

const notesFailed = createAction('NOTES_FAILED', (entity, err) => [entity, err]);
export const setCurrentEntity = createAction('NOTES_SET_CURRENT_ENTITY');
export const startDeletingNote = createAction('NOTES_START_DELETING', (entity, id) => [entity, id]);
export const stopDeletingNote = createAction('NOTES_STOP_DELETING', (entity, id) => [entity, id]);
export const startAddingNote = createAction('NOTES_START_ADDING');
export const startEditingNote = createAction('NOTES_START_EDITING', (entity, id) => [entity, id]);
export const stopAddingOrEditingNote = createAction('NOTES_STOP_EDITING_OR_ADDING');

export const notesUri = (entity, noteId) => {
  const uri = `/notes/${entity.entityType}/${entity.entityId}`;
  if (noteId != null) {
    return `${uri}/${noteId}`;
  }
  return uri;
};

export function createNote(entity, data) {
  return dispatch => {
    dispatch(stopAddingOrEditingNote(entity));
    Api.post(notesUri(entity), data)
      .then(
        json => dispatch(actions.addEntity(entity, json)),
        err => dispatch(notesFailed(entity, err))
      );
  };
}

export function editNote(entity, id, data) {
  return dispatch => {
    dispatch(stopAddingOrEditingNote(entity));
    Api.patch(notesUri(entity, id), data)
      .then(
        json => dispatch(actions.updateItems([json])),
        err => dispatch(notesFailed(entity, err))
      );
  };
}

export function deleteNote(entity, id) {
  return dispatch => {
    dispatch(stopDeletingNote(entity, id));
    Api.delete(notesUri(entity, id))
      .then(
        json => dispatch(actions.removeEntity(entity, {id})),
        err => dispatch(notesFailed(entity, err))
      );
  };
}

const initialState = {};

const notesReducer = createReducer({
  [setCurrentEntity]: (state, entity) => {
    return assoc(state, 'currentEntity', entity);
  },
  [actions.searchSuccess]: state => {
    return assoc(state, 'wasReceived', true);
  },
  [notesFailed]: (state, error) => {
    console.error(error);

    return assoc(state, 'error', error);
  },
  [startDeletingNote]: (state, id) => {
    return assoc(state, 'noteIdToDelete', id);
  },
  [stopDeletingNote]: state => {
    return dissoc(state, 'noteIdToDelete');
  },
  [startAddingNote]: state => {
    // -1 means that we adding note
    return assoc(state, 'editingNoteId', -1);
  },
  [startEditingNote]: (state, id) => {
    return assoc(state, 'editingNoteId', id);
  },
  [stopAddingOrEditingNote]: state => {
    return dissoc(state, 'editingNoteId');
  },
}, initialState);

const reduceInList = (state, action) => {
  return update(state, 'list', reducer, action);
};

const finalReducer = reduceReducers(notesReducer, reduceInList);

export {
  finalReducer as default,
  actions
};
