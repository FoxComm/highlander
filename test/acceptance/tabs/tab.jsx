import React from 'react';
import ShallowTestUtils from 'react-shallow-testutils';

describe('TabView', function() {
  const TabView = requireComponent('tabs/tab.jsx');
  const titleText = 'All';

  let tab;

  afterEach(function() {
    if (tab) {
      tab.unmount();
      tab = null;
    }
  });

  it('should contain title text', function *() {
    tab = shallowRender(
      <TabView>{ titleText }</TabView>
    );
    expect(tab.props.children, 'to contain', titleText);
  });

  it('should be draggable by default', function *() {
    tab = shallowRender(
      <TabView>{ titleText }</TabView>
    );
    expect(tab, 'to contain', <i className="icon-drag-drop" />);
  });

  it('should be draggable when property is false', function *() {
    tab = shallowRender(
      <TabView draggable={ false }>{ titleText }</TabView>
    );
    expect(tab).not.to.contain(<i className="icon-drag-drop" />);
  });
});
