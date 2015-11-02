'use strict';

import _ from 'lodash';
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';
import { assoc, dissoc, update, get } from 'sprout-data';
import { updateItems } from './state-helpers';
import { paginateReducer, actionTypes, paginate, pickFetchParams } from './pagination';

const NOTES = 'NOTES';

const receiveNotes = createAction('NOTES_RECEIVE', (entity, notes) => [entity, notes]);
const updateNotes = createAction('NOTES_UPDATE', (entity, notes) => [entity, notes]);
const noteRemoved = createAction('NOTES_REMOVED', (entity, id) => [entity, id]);
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

export function fetchNotes(entity) {
  const {entityType, entityId} = entity;

  return (dispatch, getState) => {
    const state = get(getState(), ['notes', entityType, entityId]);

    return Api.get(notesUri(entity), pickFetchParams(state))
      .then(json => dispatch(receiveNotes(entity, json)))
      .catch(err => dispatch(notesFailed(entity, err)));
  };
}

export function createNote(entity, data) {
  return dispatch => {
    dispatch(stopAddingOrEditingNote(entity));
    Api.post(notesUri(entity), data)
      .then(json => dispatch(updateNotes(entity, [json])))
      .catch(err => dispatch(notesFailed(entity, err)));
  };
}

export function editNote(entity, id, data) {
  return dispatch => {
    dispatch(stopAddingOrEditingNote(entity));
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
  [receiveNotes]: (state, [{entityType, entityId}, payload]) => {
    return update(state, [entityType, entityId], paginate, {
      type: actionTypes.FETCH,
      payload
    });
  },
  [updateNotes]: (state, [{entityType, entityId}, payload]) => {
    state = update(state, [entityType, entityId, 'rows'], updateItems, notes);
    return update(state, [entityType, entityId], paginate, {
      type: actionTypes.UPDATE_TOTAL,
      payload
    })
  },
  [notesFailed]: (state, [{entityType, entityId}, payload]) => {
    return update(state, [entityType, entityId], paginate, {
      type: actionTypes.FETCH_FAILED,
      payload
    });
  },
  [noteRemoved]: (state, [{entityType, entityId}, id]) => {
    return update(state, [entityType, entityId, 'rows'], _.reject, {id});
  },
  [startDeletingNote]: (state, [{entityType, entityId}, id]) => {
    return assoc(state, [entityType, entityId, 'noteIdToDelete'], id);
  },
  [stopDeletingNote]: (state, [{entityType, entityId}]) => {
    return dissoc(state, [entityType, entityId, 'noteIdToDelete']);
  },
  [startAddingNote]: (state, {entityType, entityId}) => {
    // true means that we adding note
    return assoc(state, [entityType, entityId, 'editingNoteId'], true);
  },
  [startEditingNote]: (state, [{entityType, entityId}, id]) => {
    return assoc(state, [entityType, entityId, 'editingNoteId'], id);
  },
  [stopAddingOrEditingNote]: (state, {entityType, entityId}) => {
    return dissoc(state, [entityType, entityId, 'editingNoteId']);
  }
}, initialState);

function paginateBehaviour(state, action, actionType) {
  const [{entityType, entityId}, payload] = action.payload;

  return update(state, [entityType, entityId], paginate, {
    ...action,
    payload,
    type: actionType
  });
}

export default paginateReducer(NOTES, reducer, paginateBehaviour);
