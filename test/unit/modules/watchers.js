import _ from 'lodash';
import nock from 'nock';

const sources = importSource('modules/watchers.js', [
  'creators',
  'actions',
  'initialState',
]);
const createStore = importSource('lib/store-creator.js');
const dsl = importSource('elastic/dsl.js');


const { actions, reducer } = createStore('', sources.actions, sources.creators, sources.initialState);


describe('watchers module', function () {

  context('reducers', function () {

    const initialState = sources.initialState;
    const group = 'watchers';

    const users = [
      {id: 1, name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
      {id: 2, name: 'Donkey Donkey', email: 'eugene@foxcommerce.com'},
      {id: 3, name: 'Eugene Donkey', email: 'eugene@foxcommerce.com'}
    ];

    it('toggleListModal should toggle list modal visibility for specified group', function () {
      const newState = reducer(initialState, actions.toggleListModal(group));
      expect(newState[group].listModalDisplayed).to.equal(true);
      const lastState = reducer(newState, actions.toggleListModal(group));
      expect(lastState[group].listModalDisplayed).to.equal(false);
    });

    it('showSelectModal should set group and update flag', function () {
      const {selectModal} = reducer(initialState, actions.showSelectModal(group));
      expect(selectModal.group).to.equal(group);
      expect(selectModal.displayed).to.equal(true);
    });

    it('hideSelectModal should reset group and update flag', function () {
      const newState = reducer(initialState, actions.showSelectModal(group));
      const {selectModal} = reducer(newState, actions.hideSelectModal());
      expect(selectModal.group).to.equal(null);
      expect(selectModal.displayed).to.equal(false);
    });

    it('selectItem should add item to selected items array', function () {
      const itemOne = users[0];
      const itemTwo = users[1];

      const stateOne = reducer(initialState, actions.selectItem(itemOne));
      expect(stateOne.selectModal.selected).to.deep.equal([itemOne]);

      const stateTwo = reducer(stateOne, actions.selectItem(itemTwo));
      expect(stateTwo.selectModal.selected).to.deep.equal([itemOne, itemTwo]);
    });

    it('deselectItem should remove item from selected items array', function () {
      const itemOne = users[0];
      const itemTwo = users[1];
      const stateOne = reducer(initialState, actions.selectItem(itemOne));
      const stateTwo = reducer(stateOne, actions.selectItem(itemTwo));
      expect(stateTwo.selectModal.selected).to.deep.equal([itemOne, itemTwo]);

      const stateThree = reducer(stateTwo, actions.deselectItem(0));
      expect(stateThree.selectModal.selected).to.deep.equal([itemTwo]);

      const stateFour = reducer(stateThree, actions.deselectItem(0));
      expect(stateFour.selectModal.selected).to.deep.equal([]);
    });

    it('clearSelected should remove all selected items', function () {
      const itemOne = users[0];
      const itemTwo = users[1];
      const stateOne = reducer(initialState, actions.selectItem(itemOne));
      const stateTwo = reducer(stateOne, actions.selectItem(itemTwo));
      expect(stateTwo.selectModal.selected).to.deep.equal([itemOne, itemTwo]);

      const stateThree = reducer(stateTwo, actions.clearSelected());
      expect(stateThree.selectModal.selected).to.deep.equal([]);
    });

  });

});
