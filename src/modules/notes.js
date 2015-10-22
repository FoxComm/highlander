'use strict';

import _ from 'lodash';
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc, dissoc } from 'sprout-data';
import { updateItems } from './state-helpers';

const requestNotes = createAction('NOTES_REQUEST');
const receiveNotes = createAction('NOTES_RECEIVE', (entity, notes) => [entity, notes]);
const receiveNotesFailed = createAction('NOTES_RECEIVE_FAILED', (entity, err) => [entity, err]);
const updateNotes = createAction('NOTES_UPDATE', (entity, notes) => [entity, notes]);
const noteRemoved = createAction('NOTES_REMOVED', (entity, id) => [entity, id]);
const notesFailed = createAction('NOTES_FAILED', (entity, err) => [entity, err]);
export const startDeletingNote = createAction('NOTES_START_DELETING', (entity, id) => [entity, id]);
export const stopDeletingNote = createAction('NOTES_STOP_DELETING', (entity, id) => [entity, id]);

const notesUri = (entity, noteId) => {
  const uri = `/notes/${entity.entityType}/${entity.entityId}`;
  if (noteId != null) {
    return `${uri}/${noteId}`;
  }
  return uri;
};

export function fetchNotes(entity) {
  return dispatch => {
    dispatch(requestNotes(entity));
    return Api.get(notesUri(entity))
      .then(json => dispatch(receiveNotes(entity, json)))
      .catch(err => dispatch(receiveNotesFailed(entity, err)));
  };
}

function shouldFetchNotes(state, entity) {
  if (!state.notes[entity.entityType]) return true;

  const notes = state.notes[entity.entityType][entity.entityId];
  if (!notes) {
    return true;
  } else if (notes.isFetching) {
    return false;
  }
  return notes.didInvalidate;
}

export function fetchNotesIfNeeded(entity) {
  return (dispatch, getState) => {
    if (shouldFetchNotes(getState(),entity)) {
      return dispatch(fetchNotes(entity));
    }
  };
}

export function createNote(entity, data) {
  return dispatch => {
    Api.post(notesUri(entity), data)
      .then(json => dispatch(updateNotes(entity, [json])))
      .catch(err => dispatch(notesFailed(entity, err)));
  };
}

export function editNote(entity, id, data) {
  return dispatch => {
    Api.patch(notesUri(entity, id), data)
      .then(json => dispatch(updateNotes(entity, [json])))
      .catch(err => dispatch(notesFailed(entity, err)));
  };
}

export function deleteNote(entity, id) {
  return dispatch => {
    dispatch(stopDeletingNote(entity, id));
    Api.delete(notesUri(entity, id))
      .then(json => dispatch(noteRemoved(entity, id)))
      .catch(err => dispatch(notesFailed(entity, err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [requestNotes]: (state, {entityType, entityId}) => {
    return assoc(state, [entityType, entityId], {
      isFetching: true,
      didInvalidate: false
    });
  },
  [receiveNotes]: (state, [{entityType, entityId}, notes]) => {
    return assoc(state, [entityType, entityId], {
      notes,
      isFetching: false,
      didInvalidate: false
    });
  },
  [updateNotes]: (state, [{entityType, entityId}, notes]) => {
    return assoc(
      state,
      [entityType, entityId, 'notes'],
      updateItems(state[entityType][entityId].notes, notes)
    );
  },
  [receiveNotesFailed]: (state, [{entityType, entityId}, err]) => {
    console.error(err);

    return assoc(state, [entityType, entityId], {
      err,
      isFetching: false,
      didInvalidate: false
    });
  },
  [notesFailed]: (state, [entity, err]) => {
    console.error(err);

    return state;
  },
  [noteRemoved]: (state, [{entityType, entityId}, id]) => {
    const restNotes = _.reject(state[entityType][entityId].notes, {id});

    return assoc(state,[entityType, entityId, 'notes'], restNotes);
  },
  [startDeletingNote]: (state, [{entityType, entityId}, id]) => {
    return assoc(state, [entityType, entityId, 'noteIdToDelete'], id);
  },
  [stopDeletingNote]: (state, [{entityType, entityId}]) => {
    return dissoc(state, [entityType, entityId, 'noteIdToDelete']);
  }
}, initialState);

export default reducer;
