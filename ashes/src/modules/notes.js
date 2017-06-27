import _ from 'lodash';
import Api from '../lib/api';
import reduceReducers from 'reduce-reducers';
import { createAction, createReducer } from 'redux-act';
import { assoc, dissoc, update } from 'sprout-data';
import makeLiveSearch from './live-search';
import processQuery from '../elastic/notes';

function geCurrentEntity(state) {
  return _.get(state, 'notes.currentEntity');
}

const {reducer, actions} = makeLiveSearch(
  'notes.list',
  [],
  'notes_search_view/_search',
  null, {
    processQuery: (query, {getState}) => {
      const currentEntity = geCurrentEntity(getState());

      return processQuery(currentEntity, query);
    },
    skipInitialFetch: true
  }
);

const notesFailed = createAction('NOTES_FAILED');
export const setCurrentEntity = createAction('NOTES_SET_CURRENT_ENTITY');
export const startDeletingNote = createAction('NOTES_START_DELETING');
export const stopDeletingNote = createAction('NOTES_STOP_DELETING');
export const startAddingNote = createAction('NOTES_START_ADDING');
export const startEditingNote = createAction('NOTES_START_EDITING');
export const stopAddingOrEditingNote = createAction('NOTES_STOP_EDITING_OR_ADDING');

export const notesUri = (entity, noteId) => {
  const uri = `/notes/${entity.entityType}/${entity.entityId}`;
  if (noteId != null) {
    return `${uri}/${noteId}`;
  }
  return uri;
};

export function createNote(data) {
  return (dispatch, getState) => {
    const entity = geCurrentEntity(getState());

    dispatch(stopAddingOrEditingNote());
    Api.post(notesUri(entity), data)
      .then(
        json => dispatch(actions.addEntity(json)),
        err => dispatch(notesFailed(err))
      );
  };
}

export function editNote(id, data) {
  return (dispatch, getState) => {
    const entity = geCurrentEntity(getState());

    dispatch(stopAddingOrEditingNote());
    Api.patch(notesUri(entity, id), data)
      .then(
        json => dispatch(actions.updateItems([json])),
        err => dispatch(notesFailed(err))
      );
  };
}

export function deleteNote(id) {
  return (dispatch, getState) => {
    const entity = geCurrentEntity(getState());

    dispatch(stopDeletingNote(id));
    Api.delete(notesUri(entity, id))
      .then(
        json => dispatch(actions.removeEntity({id})),
        err => dispatch(notesFailed(err))
      );
  };
}

const initialState = {
  isFetching: true,
};

const notesReducer = createReducer({
  [setCurrentEntity]: (state, entity) => {
    return assoc(state, 'currentEntity', entity);
  },
  [actions.searchSuccess]: state => {
    return assoc(state,
      ['isFetching'], false,
      ['wasReceived'], true);
  },
  [actions.removeEntity]: state => {
    return assoc(state, 'isFetching', false);
  },
  [actions.updateItems]: state => {
    return assoc(state, 'isFetching', false);
  },
  [actions.addEntity]: state => {
    return assoc(state, 'isFetching', false);
  },
  [actions.searchStart]: state => {
    return assoc(state, 'isFetching', true);
  },
  [notesFailed]: (state, error) => {
    console.error(error);
    return assoc(state, 'error', error);
  },
  [startDeletingNote]: (state, id) => {
    return assoc(state, 'noteIdToDelete', id);
  },
  [stopDeletingNote]: state => {
    const newState = assoc(state, 'isFetching', true);
    return dissoc(newState, 'noteIdToDelete');
  },
  [startAddingNote]: state => {
    // -1 means that we adding note
    return assoc(state,
      ['isFetching'], false,
      ['editingNoteId'], -1);
  },
  [startEditingNote]: (state, id) => {
    return assoc(state, 'editingNoteId', id);
  },
  [stopAddingOrEditingNote]: state => {
    const newState = assoc(state, 'isFetching', true);
    return dissoc(newState, 'editingNoteId');
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
