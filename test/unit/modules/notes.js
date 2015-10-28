
import reducer, * as actions from '../../../src/modules/notes';
import nock from 'nock';

describe('Notes module', function() {
  context('async actions', function() {

    const entity = {
      entityId: '123',
      entityType: 'orders'
    };

    it('fetchNotes', function (done) {
      const response = [
        {id: 1, body: 'note 1', createdAt: new Date().toGMTString()},
        {id: 2, body: 'leaving the house turn off the lights', createdAt: new Date().toGMTString()}
      ];

      nock('https://api.foxcommerce/')
        .get(`/api/v1/notes/${entity.entityType}/${entity.entityId}`)
        .reply(200, response);

      const expectedActions = [
        { type: 'NOTES_RECEIVE', payload: [entity, response]}
      ];
      const store = mockStore({}, expectedActions, done);
      store.dispatch(actions.fetchNotes(entity));
    });
  });
});
