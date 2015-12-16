import _ from 'lodash';
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc, dissoc, update, get } from 'sprout-data';
import { updateItems } from './state-helpers';

import makePagination from './pagination/structured-store';

const dataPath = ({entityType, entityId}) => [entityType, entityId];

const { makeActions, makeReducer } = makePagination('NOTES', dataPath);

const notesFailed = createAction('NOTES_FAILED', (entity, err) => [entity, err]);
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

const {
    fetch,
    actionAddEntity,
    actionRemoveEntity,
    actionReceived,
    actionUpdateItems,
  } = makeActions(notesUri);

export function createNote(entity, data) {
  return dispatch => {
    dispatch(stopAddingOrEditingNote(entity));
    Api.post(notesUri(entity), data)
      .then(
        json => dispatch(actionAddEntity(entity, json)),
        err => dispatch(notesFailed(entity, err))
      );
  };
}

export function editNote(entity, id, data) {
  return dispatch => {
    dispatch(stopAddingOrEditingNote(entity));
    Api.patch(notesUri(entity, id), data)
      .then(
        json => dispatch(actionUpdateItems(entity, [json])),
        err => dispatch(notesFailed(entity, err))
      );
  };
}

export function deleteNote(entity, id) {
  return dispatch => {
    dispatch(stopDeletingNote(entity, id));
    Api.delete(notesUri(entity, id))
      .then(
        json => dispatch(actionRemoveEntity(entity, {id})),
        err => dispatch(notesFailed(entity, err))
      );
  };
}

const initialState = {};

const notesReducer = createReducer({
  [actionReceived]: (state, [{entityType, entityId}, notes]) => {
    return assoc(state, [entityType, entityId, 'wasReceived'], true);
  },
  [notesFailed]: (state, [{entityType, entityId}, error]) => {
    console.error(error);

    return assoc(state, [entityType, entityId], {
      error
    });
  },
  [startDeletingNote]: (state, [{entityType, entityId}, id]) => {
    return assoc(state, [entityType, entityId, 'noteIdToDelete'], id);
  },
  [stopDeletingNote]: (state, [{entityType, entityId}]) => {
    return dissoc(state, [entityType, entityId, 'noteIdToDelete']);
  },
  [startAddingNote]: (state, {entityType, entityId}) => {
    // -1 means that we adding note
    return assoc(state, [entityType, entityId, 'editingNoteId'], -1);
  },
  [startEditingNote]: (state, [{entityType, entityId}, id]) => {
    return assoc(state, [entityType, entityId, 'editingNoteId'], id);
  },
  [stopAddingOrEditingNote]: (state, {entityType, entityId}) => {
    return dissoc(state, [entityType, entityId, 'editingNoteId']);
  }
}, initialState);

const reducer = makeReducer(notesReducer);

export {
  reducer as default,
  fetch as fetchNotes
};
