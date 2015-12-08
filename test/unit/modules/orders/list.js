import _ from 'lodash';
import nock from 'nock';

const ordersSearchTerms = require('../../../fixtures/orders-search-terms.js');
const makeLiveSearch = importSource('modules/live-search.js');
const { reducer: reducer, actions: actions } = makeLiveSearch('TEST', ordersSearchTerms);
const { deleteSearchFilter, goBack, submitFilter } = actions;

describe('modules.orders.list', function() {
  describe('goBack()', function() {
    let newState = null;

    context('when searching against a first-level term', function() {
      beforeEach(function() {
        newState = reducer(undefined, submitFilter(''));
      });

      it('should do nothing when there is no search term', function() {
        newState = reducer(newState, goBack());
        expect(newState.searchValue).to.be.equal('');
      });

      it('should turn a single partial term to an empty string', function() {
        const update = reducer(reducer(newState, submitFilter('Ord')), goBack());
        expect(update.searchValue).to.be.equal('');
      });
    });

    context('when searching against a second level term', function() {
      beforeEach(function() {
        newState = reducer(reducer(newState, submitFilter('Order : State')), goBack());
      });

      it('should remove the second term', function() {
        expect(newState.searchValue).to.be.equal('Order : ');
      });

      it('should update the available searches', function() {
        expect(newState.currentOptions).to.have.length(ordersSearchTerms[0].options.length);
        expect(newState.currentOptions[0].display).to.be.equal(
          ordersSearchTerms[0].options[0].displayTerm
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
        newState = reducer(undefined, submitFilter('Order : ID : 7'));
      });

      it('should create a new saved filter', function() {
        expect(newState.searches.length).to.be.equal(1);
        expect(newState.searches[0]).to.be.equal('Order : ID : 7');
      });

      it('should clear the search box', function() {
        expect(newState.searchValue).to.be.equal('');
      });
    });

    context('when submitting an invalid filter', function() {
      const invalidSearchTerm = 'Invalid Search';

      beforeEach(function() {
        newState = reducer(undefined, submitFilter(invalidSearchTerm));
      });

      it('should not save a new search', function() {
        expect(newState.searches.length).to.be.equal(0);
      });

      it('should not update the search box', function() {
        expect(newState.searchValue).to.be.equal(invalidSearchTerm);
      });
    });
  });
});
