'use strict';

import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';
import { modelIdentity } from './state-helpers';

const requestNotes = createAction('NOTES_REQUEST', (type, identity) => [type, identity]);
const receiveNotes = createAction('NOTES_RECEIVE', (type, identity, notes) => [type, identity, notes]);
const notesFailed = createAction('NOTES_FAILED', (type, identity, err) => [type, identity, err]);

export function fetchNotes(type, model) {
  return dispatch => {
    const identity = modelIdentity(type, model);
    dispatch(requestNotes(type, identity));
    return Api.get(`/notes/${type}/${identity}`)
      .then(json => dispatch(receiveNotes(type, identity, json)))
      .catch(err => dispatch(notesFailed(type, identity, err)));
  };
}

function shouldFetchNotes(state, type, model) {
  if (!state.notes[type]) return true;
  const identity = modelIdentity(type, model);

  const notes = state.notes[type][identity];
  if (!notes) {
    return true;
  } else if (notes.isFetching) {
    return false;
  }
  return notes.didInvalidate;
}

export function fetchNotesIfNeeded(type, model) {
  return (dispatch, getState) => {
    if (shouldFetchNotes(getState(), type, model)) {
      return dispatch(fetchNotes(type, model));
    }
  };
}

export function createNote(data) {

}

const initialState = {};

const reducer = createReducer({
  [requestNotes]: (state, [type, identity]) => {
    return {
      ...state,
      [type]: {
        ...state[type],
        [identity]: {
          isFetching: true,
          didInvalidate: false
        }
      }
    };
  },
  [receiveNotes]: (state, [type, identity, notes]) => {
    return {
      ...state,
      [type]: {
        ...state[type],
        [identity]: {
          notes,
          isFetching: false,
          didInvalidate: false
        }
      }
    };
  },
  [notesFailed]: (state, [type, identity, err]) => {
    console.error(err);

    return {
      ...state,
      [type]: {
        ...state[type],
        [identity]: {
          err,
          isFetching: false,
          didInvalidate: false
        }
      }
    };
  }
}, initialState);

export default reducer;
