import _ from 'lodash';
import nock from 'nock';
import util from 'util';

const ordersSearchTerms = require('../../../fixtures/orders-search-terms.js');
const makeLiveSearch = importSource('modules/live-search.js');
const { reducer: reducer, actions: actions } = makeLiveSearch('TEST', ordersSearchTerms);
const { cloneSearch, deleteSearchFilter, selectSavedSearch, submitFilter } = actions;

const selectedSearch = (state) => state.savedSearches[state.selectedSearch];

describe('modules.orders.list', function() {
  describe('cloneSearch()', function() {
    let newState = null;

    beforeEach(function() {
      newState = reducer(reducer(undefined, submitFilter('Order : ID : 5')), cloneSearch());
    });

    it('should create a new search that matched the previously selected search', function() {
      expect(selectedSearch(newState).name).to.be.equal('');
      expect(selectedSearch(newState).searches).to.have.length(1);
    });

    it('should be in a new state and editing the name', function() {
      expect(selectedSearch(newState).isEditingName).to.be.true;
      expect(selectedSearch(newState).isNew).to.be.true;
    });
  });

  describe('submitFilter()', function() {
    let newState = null;
    let searchState = null;

    context.skip('when submitting a search', function() {

    });

    context('when submitting a valid filter', function() {
      beforeEach(function() {
        newState = reducer(undefined, submitFilter('Order : ID : 7'));
        searchState = selectedSearch(newState);
      });

      it('should create a new saved filter', function() {
        expect(searchState.searches).to.have.length(1);
        expect(searchState.searches[0]).to.be.equal('Order : ID : 7');
      });

      it('should clear the search box', function() {
        expect(searchState.searchValue).to.be.equal('');
      });
    });
  });

  describe('selectSavedSearch()', function() {
    let newState = null;

    it('should have All selected by default', function() {
      newState = reducer(undefined, submitFilter(''));
      expect(newState.selectedSearch).to.be.equal(0);
    });

    it('should be able to select remorse hold', function() {
      newState = reducer(undefined, selectSavedSearch(1));
      expect(newState.selectedSearch).to.be.equal(1);
    });
  });
});
