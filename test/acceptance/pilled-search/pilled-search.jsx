import React from 'react';
import TestUtils from 'react-addons-test-utils';
import ReactDOM from 'react-dom';
import util from 'util';

describe('PilledSearch', function() {
  const PilledSearch = requireComponent('pilled-search/pilled-search.jsx');
  let pilledSearch = null;
  let searchInput = null;
  const renderedDOM = () => ReactDOM.findDOMNode(pilledSearch);

  const pressBackspace = (control) => {
    TestUtils.Simulate.keyDown(
      control,
      { key: 'Backspace', keyCode: 8, which: 8 }
    );
  };

  const pressEnter = (control) => {
    TestUtils.Simulate.keyDown(
      control, 
      { key: 'Enter', keyCode: 13, which: 13 }
    );
  };

  const pressDown = (control) => {
    TestUtils.Simulate.keyDown(
      control,
      { keyCode: 40, which: 40 }
    );
  };

  const pressUp = (control) => {
    TestUtils.Simulate.keyDown(
      control,
      { keyCode: 38, which: 38 }
    );
  };

  const searchOptions = [
    { display: 'One' },
    { display: 'Two' },
    { display: 'Three' }
  ];

  afterEach(function() {
    if (pilledSearch) {
      pilledSearch.unmount();
      pilledSearch = null;
      searchInput = null;
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

  context('when the control has a saved pill', function() {
    beforeEach(function *() {
      pilledSearch = yield renderIntoDocument(
        <PilledSearch
          pills={["search"]}
          searchOptions={searchOptions} />
      );

      searchInput = TestUtils.findRenderedDOMComponentWithClass(
        pilledSearch,
        'fc-pilled-search__input-field'
      );
    });

    it('should delete a pill backspacing an empty search box', function() {
      TestUtils.Simulate.click(searchInput);
      pressBackspace(searchInput);
      const pills = renderedDOM().querySelector('.fc-pilled-search__pill');
      expect(pills).to.be.null;
    });

    it('should delete a pill when clicking the x on the pill', function *() {
      const pill = TestUtils.findRenderedDOMComponentWithClass(
        pilledSearch,
        'fc-pilled-search__pill'
      );
      TestUtils.Simulate.click(pill);
      
      const pills = renderedDOM().querySelectorAll('.fc-pilled-search__pill');
      expect(pills).to.have.length(0);
    });
  });


  context('when the search has options to display', function() {
    beforeEach(function *() {
      pilledSearch = yield renderIntoDocument(
        <PilledSearch searchOptions={searchOptions} />
      );
      
      searchInput = TestUtils.findRenderedDOMComponentWithClass(
        pilledSearch,
        'fc-pilled-search__input-field'
      );

      TestUtils.Simulate.focus(searchInput);
    });

    it('should show the options list when clicked', function() {
      const menu = renderedDOM().querySelectorAll('.fc-menu');
      expect(menu).to.have.length(1);

      const menuItems = renderedDOM().querySelectorAll('.fc-menu-item');
      expect(menuItems).to.have.length(3);
      expect(menuItems[0].innerHTML).to.be.equal(searchOptions[0].display);
      expect(menuItems[1].innerHTML).to.be.equal(searchOptions[1].display);
      expect(menuItems[2].innerHTML).to.be.equal(searchOptions[2].display);

      const selected = renderedDOM().querySelector('.fc-menu-item.is-selected');
      expect(selected).to.be.null;
    });

    it('should select the first option when down arrow is pressed', function() {
      pressDown(searchInput);

      const selected = renderedDOM().querySelectorAll('.fc-menu-item.is-active');
      expect(selected).to.have.length(1);
      expect(selected[0].innerHTML).to.be.equal(searchOptions[0].display);
    });

    it('should never select past the last option', function() {
      pressDown(searchInput);
      pressDown(searchInput);
      pressDown(searchInput);
      pressDown(searchInput);
      pressDown(searchInput);

      const selected = renderedDOM().querySelectorAll('.fc-menu-item.is-active');
      expect(selected).to.have.length(1);
      expect(selected[0].innerHTML).to.be.equal(searchOptions[2].display);
    });

    it('should select the previous item when the up arrow is pressed', function() {
      pressDown(searchInput);
      pressDown(searchInput);
      pressUp(searchInput);

      const selected = renderedDOM().querySelectorAll('.fc-menu-item.is-active');
      expect(selected).to.have.length(1);
      expect(selected[0].innerHTML).to.be.equal(searchOptions[0].display);
    });

    it('should open the menu when the down arrow is pressed', function() {
      pressEnter(searchInput);
      pressDown(searchInput);
      const menu = renderedDOM().querySelectorAll('.fc-menu');
      expect(menu).to.have.length(1);
    });

    it('should close the menu when up arrow is pressed with no selection', function() {
      pressUp(searchInput);
      const menu = renderedDOM().querySelectorAll('.fc-menu');
      expect(menu).to.be.empty;
    });

    it('should create a pill when enter is pressed with a selected option', function() {
      pressDown(searchInput);
      pressEnter(searchInput);

      const pills = renderedDOM().querySelectorAll('.fc-pilled-search__pill span');
      expect(pills).to.have.length(1);
      expect(pills[0].innerHTML).to.be.equal(searchOptions[0].display);
    });

    it('should create a pill when clicking an option in the list', function() {
      const firstOption = TestUtils.findRenderedDOMComponentWithClass(
        pilledSearch,
        'fc-menu-item is-first'
      );

      TestUtils.Simulate.click(firstOption);

      const pills = renderedDOM().querySelectorAll('.fc-pilled-search__pill span');
      expect(pills).to.have.length(1);
      expect(pills[0].innerHTML).to.be.equal(searchOptions[0].display);
    });

    it('should do nothing when enter is pressed with no search term', function() {
      pressEnter(searchInput);
      const pills = renderedDOM().querySelectorAll('.fc-pilled-search__pill');
      expect(pills).to.be.empty;
    });

    it('should show the original search term when deselecting all options', function() {
      pressDown(searchInput);
      pressDown(searchInput);
      pressUp(searchInput);
      pressUp(searchInput);

      expect(searchInput.value).to.be.equal('');
    });
  });

  context('when the search has options and a search term to display', function() {
    beforeEach(function *() {
      pilledSearch = yield renderIntoDocument(
        <PilledSearch searchOptions={searchOptions} searchValue="Test" />
      );
      
      searchInput = TestUtils.findRenderedDOMComponentWithClass(
        pilledSearch,
        'fc-pilled-search__input-field'
      );

      TestUtils.Simulate.focus(searchInput);
    });

    it('should create a pill when enter is pressed on a search term', function() {
      pressEnter(searchInput);
      const pills = renderedDOM().querySelectorAll('.fc-pilled-search__pill');
      expect(pills).to.have.length(1);

      const menu = renderedDOM().querySelectorAll('.fc-menu');
      expect(menu).to.be.empty;
    });
  });

  context('when the search has no options to display', function() {
    beforeEach(function *() {
      pilledSearch = yield renderIntoDocument(<PilledSearch searchValue="test" />);
      searchInput = TestUtils.findRenderedDOMComponentWithClass(
        pilledSearch,
        'fc-pilled-search__input-field'
      );

      TestUtils.Simulate.focus(searchInput);
    });

    it('should not show the options list when clicked', function() {
      const menu = renderedDOM().querySelectorAll('.fc-menu');
      expect(menu).to.be.empty;
    });

    it('mousing down should not display the search options', function() {
      pressDown(searchInput);
      const menu = renderedDOM().querySelectorAll('.fc-menu');
      expect(menu).to.be.empty;
    });
  });
});
