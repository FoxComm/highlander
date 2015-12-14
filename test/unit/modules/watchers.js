import _ from 'lodash';
import nock from 'nock';

const { default: reducer, ...actions } = importSource('modules/watchers.js', [
  'toggleWatchers',
  'showAddingModal',
  'closeAddingModal',
  'itemSelected',
  'itemDeleted',
  'setSuggestedWathcers',
  'setWatchers',
  'setAssignees',
  'assignWatchers'
]);

describe('watchers module', function() {

  context('reducers', function() {

    const initialState = {};
    const entity = {entityType: 'order', enitityId: 'ABC123-13'};
    const group = 'watchers';

    const fakeAssignees = [
      {name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'}
    ];

    const fakeWatchers = [
      {name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
      {name: 'Donkey Donkey', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Donkey', email: 'eugene@foxcommerce.com'}
    ];

    it('setWatchers should insert watchers array into state', function() {
      const newState = reducer(initialState, actions.setWatchers(entity, fakeWatchers));
      const watchers = _.get(newState, [entity.entityType, entity.entityId, 'watchers', 'entries']);
      expect(watchers).to.deep.equal(fakeWatchers);
    });

    it('setAssignees should insert assignees array into state', function() {
      const newState = reducer(initialState, actions.setAssignees(entity, fakeAssignees));
      const assignees = _.get(newState, [entity.entityType, entity.entityId, 'assignees', 'entries']);
      expect(assignees).to.deep.equal(fakeAssignees);
    });

    it('toggleWatchers should change state of displayed flag for a group', function() {
      const newState = reducer(initialState, actions.toggleWatchers(entity, group));
      const newValue = _.get(newState, [entity.entityType, entity.entityId, group, 'displayed']);
      expect(newValue).to.be.equal(true);
      const updatedTwiceState = reducer(newState, actions.toggleWatchers(entity, group));
      const updatedTwiceValue = _.get(updatedTwiceState, [entity.entityType, entity.entityId, group, 'displayed']);
      expect(updatedTwiceValue).to.be.equal(false);
    });

    it('showAddingModal should update flag and set group', function() {
      const newState = reducer(initialState, actions.showAddingModal(entity, group));
      const flag = _.get(newState, [entity.entityType, entity.entityId, 'modalDisplayed']);
      const groupSet = _.get(newState, [entity.entityType, entity.entityId, 'modalGroup']);
      expect(flag).to.be.equal(true);
      expect(groupSet).to.be.equal(group);
    });

    it('closeAddingModal should update flag and reset group', function() {
      const newState = reducer(initialState, actions.closeAddingModal(entity, group));
      const flag = _.get(newState, [entity.entityType, entity.entityId, 'modalDisplayed']);
      const groupSet = _.get(newState, [entity.entityType, entity.entityId, 'modalGroup']);
      expect(flag).to.be.equal(false);
      expect(groupSet).to.be.equal(null);
    });

    it('setSuggestedWathcers should set array of suggested watchers in state', function() {
      const newState = reducer(initialState, actions.setSuggestedWathcers(entity, fakeWatchers));
      const watchers = _.get(newState, [entity.entityType, entity.entityId, 'suggestedWatchers']);
      expect(watchers).to.deep.equal(fakeWatchers);
    });

    it('assignWatchers should reset state of modal', function() {
      const newState = reducer(initialState, actions.assignWatchers(entity));
      const modalGroup = _.get(newState, [entity.entityType, entity.entityId, 'modalGroup']);
      const selectedItems = _.get(newState, [entity.entityType, entity.entityId, 'selectedItems']);
      expect(modalGroup).to.be.equal(null);
      expect(selectedItems).to.deep.equal([]);
    });

    it('itemSelected should add item to selected items array', function() {
      const item = {name: 'Donkey', email: 'donkey@foxcommerce.com'};
      const newState = reducer(initialState, actions.itemSelected(entity, item));
      const selected = _.get(newState, [entity.entityType, entity.entityId, 'selectedItems']);
      expect(selected).to.deep.equal([item]);
      const newState2 = reducer(newState, actions.itemSelected(entity, item));
      const selected2 = _.get(newState2, [entity.entityType, entity.entityId, 'selectedItems']);
      expect(selected2).to.deep.equal([item, item]);
    });
  });

});
