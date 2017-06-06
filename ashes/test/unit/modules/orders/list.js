const ordersSearchTerms = require('../../../fixtures/orders-search-terms.js');
const ordersSavedSearches = require('../../../fixtures/orders-saved-searches.js');
const makeLiveSearch = require('modules/live-search/index.js').default;
const { reducer, actions } = makeLiveSearch('TEST', ordersSearchTerms, ordersSavedSearches);
const { selectSavedSearch, submitFilters } = actions;

const selectedSearch = state => state.savedSearches[state.selectedSearch];

xdescribe('modules.orders.list', function() {
  const sampleFilter = {
    display: 'Order : ID : 7',
    term: 'id',
    operator: 'eq',
    value: {
      type: 'number',
      value: 7,
    },
  };

  describe('submitFilters()', function() {
    let newState = null;
    let searchState = null;

    context.skip('when submitting a search', function() {});

    context('when submitting a valid filter', function() {
      beforeEach(function() {
        newState = reducer(undefined, submitFilters([sampleFilter]));
        searchState = selectedSearch(newState);
      });

      it('should create a new saved filter', function() {
        expect(searchState.searches).to.have.length(1);
        expect(searchState.searches[0].display).to.be.equal('Order : ID : 7');
      });

      it('should clear the search box', function() {
        expect(searchState.searchValue).to.be.equal('');
      });
    });
  });

  describe('selectSavedSearch()', function() {
    let newState = null;

    it('should have All selected by default', function() {
      newState = reducer(undefined, submitFilters([]));
      expect(newState.selectedSearch).to.be.equal(0);
    });

    it('should be able to select remorse hold', function() {
      newState = reducer(undefined, selectSavedSearch(1));
      expect(newState.selectedSearch).to.be.equal(1);
    });
  });
});
