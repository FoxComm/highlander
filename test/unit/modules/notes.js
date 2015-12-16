
import _ from 'lodash';
import nock from 'nock';

const {default: reducer, ...actions} = importSource('modules/notes.js', [
  'actionReceived',
  'actionUpdateItems',
  'actionAddEntity',
  'actionRemoveEntity'
]);

describe('Notes module', function() {

  const entity = {
    entityId: '123',
    entityType: 'orders'
  };

  const notesPayload = [
    {id: 1, body: 'note 1', createdAt: new Date().toString()},
    {id: 2, body: 'leaving the house turn off the lights', createdAt: new Date().toString()}
  ];
  const notePayload = {
    id: 2, body: 'updated'
  };

  context('async actions', function() {

    function notesUri(entity, id = void 0) {
      return `/api/v1${actions.notesUri(entity, id)}`;
    }

    before(function() {
      const uri = notesUri(entity);

      nock(phoenixUrl)
        .get(uri)
        .reply(200, notesPayload)
        .post(uri, notePayload)
        .reply(201, notePayload);
    });

    after(function() {
      nock.cleanAll();
    });

    it('createNote', function *() {
      const expectedActions = [
        actions.stopAddingOrEditingNote,
        {type: actions.actionAddEntity, payload: [entity, notePayload]}
      ];

      yield expect(actions.createNote(entity, notePayload), 'to dispatch actions', expectedActions);
    });

    it('editNote', function *() {
      const expectedActions = [
        actions.stopAddingOrEditingNote,
        {type: actions.actionUpdateItems, payload: [entity, [notePayload]]}
      ];

      nock(phoenixUrl)
        .patch(notesUri(entity, 1))
        .reply(200, notePayload);

      yield expect(actions.editNote(entity, 1, notePayload), 'to dispatch actions', expectedActions);
    });

    it('deleteNote', function *() {
      const expectedActions = [
        { type: actions.stopDeletingNote, payload: [entity, 1]},
        { type: actions.actionRemoveEntity, payload: [entity, {id: 1}]}
      ];

      nock(phoenixUrl)
        .delete(notesUri(entity, 1))
        .reply(204, {});

      yield expect(actions.deleteNote(entity, 1), 'to dispatch actions', expectedActions);
    });
  });

  context('reducer', function() {
    const state = {
      [entity.entityType]: {
        [entity.entityId]: {
          rows: notesPayload
        }
      }
    };

    it('should update exists notes', function() {
      const newState = reducer(state, actions.actionUpdateItems(entity, [notePayload]));

      expect(_.get(newState, [entity.entityType, entity.entityId, 'rows', 1]), 'to satisfy', notePayload);
    });

    it('should remove notes', function() {
      const newState = reducer(state, actions.actionRemoveEntity(entity, {id: 1}));

      expect(_.get(newState, [entity.entityType, entity.entityId, 'rows']), 'to have length', 1);
      expect(_.get(newState, [entity.entityType, entity.entityId, 'rows']), 'to equal', [notesPayload[1]]);
    });
  });
});
