import _ from 'lodash';
import nock from 'nock';

const { default: reducer, ...actions } = requireSource('modules/notes.js');
const { actions: searchActions } = actions;

describe('Notes module', function() {
  const entity = {
    entityId: '123',
    entityType: 'orders',
  };

  const initialState = {
    notes: {
      currentEntity: entity,
    },
  };

  const notesPayload = [
    { id: 1, body: 'note 1', createdAt: new Date().toString() },
    { id: 2, body: 'leaving the house turn off the lights', createdAt: new Date().toString() },
  ];
  const notePayload = {
    id: 2,
    body: 'updated',
  };

  context('async actions', function() {
    function notesUri(entity, id = void 0) {
      return `/api/v1${actions.notesUri(entity, id)}`;
    }

    before(function() {
      const uri = notesUri(entity);

      nock(process.env.API_URL).get(uri).reply(200, notesPayload).post(uri, notePayload).reply(201, notePayload);
    });

    after(function() {
      nock.cleanAll();
    });

    it('createNote', function*() {
      const expectedActions = [
        actions.stopAddingOrEditingNote,
        { type: searchActions.addEntity, payload: notePayload },
      ];

      yield expect(actions.createNote(notePayload), 'to dispatch actions', expectedActions, initialState);
    });

    it('editNote', function*() {
      const expectedActions = [
        actions.stopAddingOrEditingNote,
        { type: searchActions.updateItems, payload: [notePayload] },
      ];

      nock(process.env.API_URL).patch(notesUri(entity, 1)).reply(200, notePayload);

      yield expect(actions.editNote(1, notePayload), 'to dispatch actions', expectedActions, initialState);
    });

    it('deleteNote', function*() {
      const expectedActions = [
        { type: actions.stopDeletingNote, payload: 1 },
        { type: searchActions.removeEntity, payload: { id: 1 } },
      ];

      nock(process.env.API_URL).delete(notesUri(entity, 1)).reply(204, {});

      yield expect(actions.deleteNote(1), 'to dispatch actions', expectedActions, initialState);
    });
  });

  xcontext('reducer', function() {
    const state = {
      [entity.entityType]: {
        [entity.entityId]: {
          rows: notesPayload,
        },
      },
    };

    it('should update exists notes', function() {
      const newState = reducer(state, searchActions.updateItems(entity, [notePayload]));

      expect(_.get(newState, [entity.entityType, entity.entityId, 'rows', 1]), 'to satisfy', notePayload);
    });

    it('should remove notes', function() {
      const newState = reducer(state, searchActions.removeEntity(entity, { id: 1 }));

      expect(_.get(newState, [entity.entityType, entity.entityId, 'rows']), 'to have length', 1);
      expect(_.get(newState, [entity.entityType, entity.entityId, 'rows']), 'to equal', [notesPayload[1]]);
    });
  });
});
