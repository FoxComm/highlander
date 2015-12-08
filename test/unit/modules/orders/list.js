import _ from 'lodash';
import nock from 'nock';
import util from 'util';

const ordersSearchTerms = require('../../../fixtures/orders-search-terms.js');
const makeLiveSearch = importSource('modules/live-search.js');
const { reducer: reducer, actions: actions } = makeLiveSearch('TEST', ordersSearchTerms);
const { deleteSearchFilter, goBack, submitFilter } = actions;

const selectedSearch = (state) => state.savedSearches[state.selectedSearch];

describe('modules.orders.list', function() {
  describe('goBack()', function() {
    let newState = null;
    let searchState = null;

    context('when searching against a first-level term', function() {
      beforeEach(function() {
        newState = reducer(undefined, submitFilter(''));
      });

      it('should do nothing when there is no search term', function() {
        newState = reducer(newState, goBack());
        expect(selectedSearch(newState).searchValue).to.be.equal('');
      });

      it('should turn a single partial term to an empty string', function() {
        const update = reducer(reducer(newState, submitFilter('Ord')), goBack());
        expect(selectedSearch(update).searchValue).to.be.equal('');
      });
    });

    context('when searching against a second level term', function() {
      beforeEach(function() {
        newState = reducer(reducer(newState, submitFilter('Order : State')), goBack());
        searchState = selectedSearch(newState);
      });

      it('should remove the second term', function() {
        expect(searchState.searchValue).to.be.equal('Order : ');
      });

      it('should update the available searches', function() {
        expect(searchState.currentOptions).to.have.length(ordersSearchTerms[0].options.length);
        expect(searchState.currentOptions[0].display).to.be.equal(
          ordersSearchTerms[0].options[0].displayTerm
        );
      });
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

    context('when submitting an invalid filter', function() {
      const invalidSearchTerm = 'Invalid Search';

      beforeEach(function() {
        newState = reducer(undefined, submitFilter(invalidSearchTerm));
        searchState = selectedSearch(newState);
      });

      it('should not save a new search', function() {
        expect(searchState.searches).to.have.length(0);
      });

      it('should not update the search box', function() {
        expect(searchState.searchValue).to.be.equal(invalidSearchTerm);
      });
    });
  });
});
