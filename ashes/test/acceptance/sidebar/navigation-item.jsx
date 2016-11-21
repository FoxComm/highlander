
import _ from 'lodash';
import React from 'react';
import * as ShallowTestUtils from 'react-shallow-testutils';

describe('NavigationItem', function() {

  const NavigationItem = requireComponent('sidebar/navigation-item.jsx');
  const Link = requireComponent('link/link.jsx');

  let navigationItem;

  const defaultProps = {
    isExpandable: true,
    to: 'donkeys',
    icon: 'icon-donkey',
    title: 'Donkeys',
    children: [<a>Donkey 1</a>, <a>Donkey 2</a>],
    routes: [{name: 'donkeys'}, {name: 'donkey-details'}],
    toggleMenuItem: _.noop
  };

  afterEach(function() {
    if (navigationItem) {
      navigationItem.unmount();
      navigationItem = null;
    }
  });

  context('collapse behavior', function() {

    it('should be closed if side bar is collapsed', function *() {
      navigationItem = shallowRender(
        <NavigationItem collapsed={true} status={{isOpen: true}} {...defaultProps}/>
      );
      expect(ShallowTestUtils.findAllWithClass(navigationItem, 'fc-navigation-item__children _open')).to.be.empty;
    });

    it('should be opened if side bar is not collapsed', function *() {
      navigationItem = shallowRender(
        <NavigationItem collapsed={false} status={{isOpen: true}} {...defaultProps}/>
      );
      expect(ShallowTestUtils.findAllWithClass(navigationItem, 'fc-navigation-item__children _open')).not.to.be.empty;
    });

    it('should be closed if side bar is not collapsed and it is closed manually', function *() {
      navigationItem = shallowRender(
        <NavigationItem collapsed={false} status={{isOpen: false, toggledManually: true}} {...defaultProps}>
          <Link to="donkeys">Donkeys</Link>
          <Link to="donkeys-donkeys">Donkeys</Link>
        </NavigationItem>
      );
      expect(ShallowTestUtils.findAllWithClass(navigationItem, 'fc-navigation-item__children _open')).to.be.empty;
    });

    it('should be closed if side bar is not collapsed and it is not closed manually', function *() {
      navigationItem = shallowRender(
        <NavigationItem collapsed={false} status={{isOpen: false, toggledManually: false}} {...defaultProps}>
          <Link to="donkeys">Donkeys</Link>
          <Link to="donkeys-donkeys">Donkeys</Link>
        </NavigationItem>
      );
      expect(ShallowTestUtils.findAllWithClass(navigationItem, 'fc-navigation-item__children _open')).not.to.be.empty;
    });

  });

});
