import _ from 'lodash';

const makeWatchers = importSource('modules/watchers.js');

const entityType = 'orders';
const entityId = 'BR0001';

const { actions, reducer } = makeWatchers(entityType, {});


describe('watchers module', function () {

  context('reducers', function () {

    const initialState = {};
    const group = 'watchers';

    const users = [
      {id: 1, name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
      {id: 2, name: 'Donkey Donkey', email: 'eugene@foxcommerce.com'},
      {id: 3, name: 'Eugene Donkey', email: 'eugene@foxcommerce.com'}
    ];

    it('toggleListModal should toggle list modal visibility for specified group', function () {
      const newState = reducer(initialState, actions.toggleListModal(entityId, group));
      expect(newState[entityId][group].listModalDisplayed).to.equal(true);
      const lastState = reducer(newState, actions.toggleListModal(entityId, group));
      expect(lastState[entityId][group].listModalDisplayed).to.equal(false);
    });

    it('showSelectModal should set group and update flag', function () {
      const {selectModal} = reducer(initialState, actions.showSelectModal(entityId, group))[entityId];
      expect(selectModal.group).to.equal(group);
      expect(selectModal.displayed).to.equal(true);
    });

    it('hideSelectModal should reset group and update flag', function () {
      const newState = reducer(initialState, actions.showSelectModal(entityId, group));
      const {selectModal} = reducer(newState, actions.hideSelectModal(entityId))[entityId];
      expect(selectModal.group).to.equal(null);
      expect(selectModal.displayed).to.equal(false);
    });

    it('selectItem should add item to selected items array', function () {
      const itemOne = users[0];
      const itemTwo = users[1];

      const stateOne = reducer(initialState, actions.selectItem(entityId, itemOne));
      expect(stateOne[entityId].selectModal.selected).to.deep.equal([itemOne]);

      const stateTwo = reducer(stateOne, actions.selectItem(entityId, itemTwo));
      expect(stateTwo[entityId].selectModal.selected).to.deep.equal([itemOne, itemTwo]);
    });

    it('deselectItem should remove item from selected items array', function () {
      const itemOne = users[0];
      const itemTwo = users[1];
      const stateOne = reducer(initialState, actions.selectItem(entityId, itemOne));
      const stateTwo = reducer(stateOne, actions.selectItem(entityId, itemTwo));
      expect(stateTwo[entityId].selectModal.selected).to.deep.equal([itemOne, itemTwo]);

      const stateThree = reducer(stateTwo, actions.deselectItem(entityId, 0));
      expect(stateThree[entityId].selectModal.selected).to.deep.equal([itemTwo]);

      const stateFour = reducer(stateThree, actions.deselectItem(entityId, 0));
      expect(stateFour[entityId].selectModal.selected).to.deep.equal([]);
    });

    it('clearSelected should remove all selected items', function () {
      const itemOne = users[0];
      const itemTwo = users[1];
      const stateOne = reducer(initialState, actions.selectItem(entityId, itemOne));
      const stateTwo = reducer(stateOne, actions.selectItem(entityId, itemTwo));
      expect(stateTwo[entityId].selectModal.selected).to.deep.equal([itemOne, itemTwo]);

      const stateThree = reducer(stateTwo, actions.clearSelected(entityId));
      expect(stateThree[entityId].selectModal.selected).to.deep.equal([]);
    });

  });

});
