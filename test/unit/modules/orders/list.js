import _ from 'lodash';
import nock from 'nock';

const { reducer, ...actions } = importModule('orders/list.js', [
  'updateSearch',
  'selectDown',
  'selectUp',
  'goBack',
  'submitFilter'
]);

describe('modules.orders.list', function() {
  const orders = require('../../../fixtures/orders.json');
  const ordersSearchTerms = require('../../../fixtures/orders-search-terms.js');

  const initialState = {
    currentOptions: [],
    isVisible: false,
    potentialOptions: [],
    inputValue: '',
    displayValue: '',
    selectedIndex: -1,
    searches: []
  };

  describe('updateSearch()', function() {
    let newState = null;

    context('applying an empty search', function() {
      beforeEach(function() {
        newState = reducer(initialState, actions.updateSearch('', ordersSearchTerms));
      });

      it('should show all first level options', function() {
        expect(newState.currentOptions.length).to.be.equal(ordersSearchTerms.length);
        _.forEach(ordersSearchTerms, (st, idx) => {
          expect(newState.currentOptions[idx].display).to.be.equal(st.term);
        });
      });

      it('should show the dropdown with nothing selected', function() {
        expect(newState.isVisible).to.be.equal(true);
        expect(newState.selectedIndex).to.be.equal(-1);
      });
    });

    context('apply a partially matching search', function() {
      beforeEach(function() {
        newState = reducer(initialState, actions.updateSearch('Ord', ordersSearchTerms));
      });

      it('should show only the matching options in the first level', function() {
        expect(newState.currentOptions[0].display).to.be.equal('Order');
      });

      it('should show the dropdown with nothing selected', function() {
        expect(newState.isVisible).to.be.equal(true);
        expect(newState.selectedIndex).to.be.equal(-1);
      });

      it('should update the search box with the correct term', function() {
        expect(newState.displayValue).to.be.equal('Ord');
        expect(newState.inputValue).to.be.equal('Ord');
      });
    });

    context('apply a fully matching search', function() {
      it('should show only the matching first level term', function() {
        newState = reducer(
          initialState,
          actions.updateSearch('Shipment', ordersSearchTerms)
        );

        expect(newState.currentOptions.length).to.be.equal(1);
        expect(newState.currentOptions[0].display).to.be.equal('Shipment');
      });
    });

    context('apply a search that displays sub-options', function() {
      beforeEach(function() {
        newState = reducer(
          initialState,
          actions.updateSearch('Order :', ordersSearchTerms)
        );
      });

      it('should show the matching sub-options if search term ends in a colon', function() {
        expect(newState.currentOptions.length).to.be.equal(
          ordersSearchTerms[0].options.length
        );
      });
    });
  });

  describe('selectDown()', function() {
    let newState = null;

    context('when there are no visible search options', function() {
      it('should not change the selectedIndex', function() {
        newState = reducer(initialState, actions.selectDown());
        expect(newState.selectedIndex).to.be.equal(-1);
      });
    });

    context('when there are multiple search options', function() {
      beforeEach(function() {
        newState = reducer(initialState, actions.updateSearch('', ordersSearchTerms));
      });

      it('should select the first item in the list', function() {
        const downOne = reducer(newState, actions.selectDown());
        expect(downOne.selectedIndex).to.be.equal(0);
      });

      it('should not be able to select past the last item in the list', function() {
        const maxIndex = newState.currentOptions.length - 1;
        const modifiedState = { ...newState, selectedIndex: maxIndex };

        const downOne = reducer(modifiedState, actions.selectDown());
        expect(downOne.selectedIndex).to.be.equal(maxIndex);
      });
    });

    context('when the search options are hidden', function() {
      it('should show the search box', function() {
        newState = reducer(
          reducer(initialState, actions.updateSearch('', ordersSearchTerms)),
          actions.selectDown()
        );
        expect(newState.isVisible).to.be.equal(true);
      });
    });
  });  

  describe('selectUp()', function() {
    let newState = null;

    context('when there are multiple search options', function() {
      beforeEach(function() {
        newState = reducer(initialState, actions.updateSearch('', ordersSearchTerms));
      });

      it('should collapse the menu when nothing is selected', function() {
        newState = reducer(newState, actions.selectUp());
        expect(newState.isVisible).to.be.equal(false);
      });

      it('should move selection up the list', function() {
        const maxIndex = newState.currentOptions.length - 1;
        const modifiedState = { ...newState, selectedIndex: maxIndex };

        const downOne = reducer(modifiedState, actions.selectUp());
        expect(downOne.selectedIndex).to.be.equal(maxIndex - 1);
      });
    });

    context('when the search options are hidden', function() {
      it('should show the search box', function() {
        newState = reducer(
          reducer(initialState, actions.updateSearch('', ordersSearchTerms)),
          actions.selectDown()
        );
        expect(newState.isVisible).to.be.equal(true);
      });
    });
  });  

  describe('goBack()', function() {
    let newState = null;

    context('when searching against a first-level term', function() {
      beforeEach(function() {
        newState = reducer(initialState, actions.updateSearch('', ordersSearchTerms));
      });

      it('should do nothing when there is no search term', function() {
        newState = reducer(newState, actions.goBack());
        expect(newState.inputValue).to.be.equal('');
      });

      it('should turn a single partial term to an empty string', function() {
        let update = {
          ...newState,
          inputValue: 'Ord',
          displayValue: 'Ord',
        };
        update = reducer(update, actions.goBack());
        expect(update.inputValue).to.be.equal('');
      });
    });

    context('when searching against a second level term', function() {
      beforeEach(function() {
        newState = reducer(
          reducer(
            initialState,
            actions.updateSearch('Order : State', ordersSearchTerms)
          ),
          actions.goBack()
        );
      });

      it('should remove the second term', function() {
        expect(newState.inputValue).to.be.equal('Order : ');
      });

      it('should update the available searches', function() {
        expect(newState.currentOptions.length).to.be.equal(
          ordersSearchTerms[0].options.length
        );
        expect(newState.currentOptions[0].display).to.be.equal(
          `Order : ${ordersSearchTerms[0].options[0].term}`
        );
      });
    });
  });

  describe('submitFilter()', function() {
    let newState = null;

    context.skip('when submitting a search', function() {

    });

    context('when submitting a valid filter', function() {
      beforeEach(function() {
        newState = reducer(
          reducer(
            initialState,
            actions.updateSearch('Order : ID : 7', ordersSearchTerms)
          ),
          actions.submitFilter()
        );
      });

      it('should create a new saved filter', function() {
        expect(newState.searches.length).to.be.equal(1);
        expect(newState.searches[0]).to.be.equal('Order : ID : 7');
      });

      it('should clear the search box', function() {
        expect(newState.inputValue).to.be.equal('');
        expect(newState.displayValue).to.be.equal('');
      });

      it('should collapse the search options', function() {
        expect(newState.isVisible).to.be.equal(false);
      });
    });

    context('when submitting an invalid filter', function() {
      const invalidSearchTerm = 'Invalid Search';

      beforeEach(function() {
        newState = reducer(
          reducer(
            initialState,
            actions.updateSearch(invalidSearchTerm, ordersSearchTerms)
          ),
          actions.submitFilter()
        );
      });

      it('should not save a new search', function() {
        expect(newState.searches.length).to.be.equal(0);
      });

      it('should not update the search box', function() {
        expect(newState.inputValue).to.be.equal(invalidSearchTerm);
        expect(newState.displayValue).to.be.equal(invalidSearchTerm);
      });
    });

    context('when selecting an incomplete filter from the list', function() {
      beforeEach(function() {
        newState = reducer(
          reducer(
            reducer(initialState, actions.updateSearch('', ordersSearchTerms)),
            actions.selectDown()
          ),
          actions.submitFilter()
        );
      });

      it('should update the contents of the search box', function() {
        const searchValue = `${ordersSearchTerms[0].term} : `;
        expect(newState.inputValue).to.be.equal(searchValue);
        expect(newState.displayValue).to.be.equal(searchValue);
      });

      it('should re-calculate the search options', function() {
        expect(newState.currentOptions.length).to.be.equal(
          ordersSearchTerms[0].options.length
        );
        _.forEach(newState.currentOptions, (option, idx) => {
          const expected = `${ordersSearchTerms[0].term} : ${ordersSearchTerms[0].options[idx].term}`;
          expect(option.display).to.be.equal(expected);
        });
      });

      it('should not update the number of searches', function() {
        expect(newState.searches.length).to.be.equal(0);
      });
    });

    context('when selecting a complete filter from the list', function() {
      beforeEach(function() {
        newState = reducer(
          reducer(
            reducer(
              initialState,
              actions.updateSearch('Order : State : ', ordersSearchTerms)
            ),
            actions.selectDown()
          ),
          actions.submitFilter()
        );
      });

      it('should create a new saved filter', function() {
        expect(newState.searches.length).to.be.equal(1);
      });

      it('should clear the search box', function() {
        expect(newState.inputValue).to.be.equal('');
        expect(newState.displayValue).to.be.equal('');
      });

      it('should collapse the search options', function() {
        expect(newState.isVisible).to.be.equal(false);
      });
    });
  }); 
});
