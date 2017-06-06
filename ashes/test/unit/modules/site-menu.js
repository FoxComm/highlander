import _ from 'lodash';

const { default: reducer, ...actions } = importSource('modules/site-menu.js', ['toggleSiteMenu', 'toggleMenuItem']);

describe('reasons module', function() {
  context('reducers', function() {
    it('toggleSiteMenu should change state of menu', function() {
      const initialState = {};
      const result = reducer(initialState, actions.toggleSiteMenu());
      expect(result.isMenuExpanded).to.be.false;
      const newResult = reducer(result, actions.toggleSiteMenu());
      expect(newResult.isMenuExpanded).to.be.true;
    });

    it('toggleMenuItem should change state of menu', function() {
      const initialState = {};
      const item = 'customers';
      const result = reducer(initialState, actions.toggleMenuItem(item));
      expect(_.get(result, ['menuItems', item, 'isOpen'])).to.be.true;
      const newResult = reducer(result, actions.toggleMenuItem(item));
      expect(_.get(newResult, ['menuItems', item, 'isOpen'])).to.be.false;
      const thirdResult = reducer(newResult, actions.toggleMenuItem(item));
      expect(_.get(thirdResult, ['menuItems', item, 'isOpen'])).to.be.true;
    });
  });
});
