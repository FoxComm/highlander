'use strict';

import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';
import { makeEntityId, updateItems } from './state-helpers';

const requestNotes = createAction('NOTES_REQUEST');
const receiveNotes = createAction('NOTES_RECEIVE', (entity, notes) => [entity, notes]);
const receiveNotesFailed = createAction('NOTES_RECEIVE_FAILED', (entity, err) => [entity, err]);
const updateNotes = createAction('NOTES_UPDATE', (entity, notes) => [entity, notes]);
const notesFailed = createAction('NOTES_FAILED', (entity, err) => [entity, err]);

const notesUri = entity => `/notes/${entity.entityType}/${entity.entityId}`;

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

const initialState = {};

const reducer = createReducer({
  [requestNotes]: (state, {entityId, entityType}) => {
    return {
      ...state,
      [entityType]: {
        ...state[entityType],
        [entityId]: {
          isFetching: true,
          didInvalidate: false
        }
      }
    };
  },
  [receiveNotes]: (state, [{entityId, entityType}, notes]) => {
    return {
      ...state,
      [entityType]: {
        ...state[entityType],
        [entityId]: {
          notes,
          isFetching: false,
          didInvalidate: false
        }
      }
    };
  },
  [updateNotes]: (state, [{entityId, entityType}, notes]) => {
    return {
      ...state,
      [entityType]: {
        ...state[entityType],
        notes: updateItems(state[entityType][entityId].notes, notes, makeEntityId(entityType))
      }
    };
  },
  [receiveNotesFailed]: (state, [{entityId, entityType}, err]) => {
    console.error(err);

    return {
      ...state,
      [entityType]: {
        ...state[entityType],
        [entityId]: {
          err,
          isFetching: false,
          didInvalidate: false
        }
      }
    };
  },
  [notesFailed]: (state, [entity, err]) => {
    console.error(err);
  }
}, initialState);

export default reducer;
