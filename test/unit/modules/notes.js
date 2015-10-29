
import _ from 'lodash';
import reducer, * as actions from '../../../src/modules/notes';
import nock from 'nock';

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

    before(function() {
      const uri = `/api/v1/notes/${entity.entityType}/${entity.entityId}`;

      nock(phoenixUrl)
        .get(uri)
        .reply(200, notesPayload)
        .post(uri, notePayload)
        .reply(201, notePayload);
    });

    it('fetchNotes', function() {
      const expectedActions = [
        { type: 'NOTES_RECEIVE', payload: [entity, notesPayload]}
      ];

      return expect(actions.fetchNotes(entity), 'to dispatch actions', expectedActions);
    });

    it('createNote', function() {
      const expectedActions = [
        actions.stopAddingOrEditingNote,
        { type: 'NOTES_UPDATE', payload: [entity, [notePayload]]}
      ];

      return expect(actions.createNote(entity, notePayload), 'to dispatch actions', expectedActions);
    });
  });

  context('reducer', function() {
    it('should update exists notes', function() {
      const state = {
        [entity.entityType]: {
          [entity.entityId]: {
            notes: notesPayload
          }
        }
      };

      const newState = reducer(state, actions.updateNotes(entity, [notePayload]));

      expect(_.get(newState, [entity.entityType, entity.entityId, 'notes', 1]), 'to satisfy', notePayload);
    });
  });
});
