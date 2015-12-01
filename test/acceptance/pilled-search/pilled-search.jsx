import React from 'react';
import TestUtils from 'react-addons-test-utils';
import ReactDOM from 'react-dom';

describe('PilledSearch', function() {
  const PilledSearch = requireComponent('pilled-search/pilled-search.jsx');
  let pilledSearch = null;
  const searchOptions = [
    { display: 'One' },
    { display: 'Two' },
    { display: 'Three' }
  ];

  afterEach(function() {
    if (pilledSearch) {
      pilledSearch.unmount();
      pilledSearch = null;
    }
  });

  it('should render', function *() {
    pilledSearch = yield renderIntoDocument(
      <PilledSearch searchOptions={searchOptions} />
    );

    const pilledSearchNode = TestUtils.findRenderedDOMComponentWithClass(
      pilledSearch,
      'fc-pilled-search'
    );

    expect(pilledSearchNode).to.be.instanceof(Object);
    expect(pilledSearchNode.className).to.contain('fc-pilled-search');
  });

  it('should create a pill when enter is pressed on a search term', function() {
  });

  it('should delete the most recent pill when backspace is pressed with no search term', function() {
  });

  it('should delete a pill when clicking the x on the pill', function() {
  });

  context('when the search has options to display', function() {
    it('should show the options list when clicked', function() {
    });

    it('should select the first option when down arrow is pressed', function() {
    });

    it('should never select past the last option', function() {
    });

    it('should select the previous item when the up arrow is pressed', function() {
    });

    it('should open the menu when the down arrow is pressed', function() {
    });

    it('should close the menu when the up arrow is pressed with no selection', function() {
    });

    it('should create a pill when enter is pressed with a selected option', function() {
    });

    it('should create a pill when clicking an option in the list', function() {
    });

    it('should do nothing when enter is pressed with no search term', function() {
    });

    it('should show the original search term when deselecting all options', function() {
    });
  });

  context('when the search has no options to display', function() {
    it('should not show the options list when clicked', function() {
    });

    it('mousing down should not display the search options', function() {
    });
  });
});
