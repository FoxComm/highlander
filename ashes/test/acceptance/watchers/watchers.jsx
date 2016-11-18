import _ from 'lodash';
import React from 'react';
import * as ShallowTestUtils from 'react-shallow-testutils';
import { groups } from '../../../src/paragons/watcher';

describe('Watchers', function () {
  const Watchers = requireComponent('watchers/watchers.jsx').WrappedComponent;

  let watchers;
  const entity = {
    entityType: 'orders',
    entityId: 'BR0001',
  };
  const data = {};
  const isFetching = {
    [groups.assignees]: false,
    [groups.watchers]: false,
  };

  const actions = {
    showSelectModal: _.noop,
    hideSelectModal: _.noop,
    toggleListModal: _.noop,
    addWatchers: _.noop,
    removeWatcher: _.noop,
  };

  afterEach(function () {
    if (watchers) {
      watchers.unmount();
      watchers = null;
    }
  });

  it('should render message when assignees are empty', function *() {
    watchers = shallowRender(
      <Watchers entity={entity} data={data} isFetching={isFetching} {...actions} />
    );
    const box = ShallowTestUtils.findWithClass(watchers, "fc-watchers__assignees-empty");
    expect(box).not.to.be.empty;
    expect(box, 'to contain', 'Unassigned');
  });

  it('should render assignee cells for each assignee in array', function *() {
    const assignees = [
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'}
    ];

    watchers = shallowRender(
      <Watchers entity={entity} data={{...data, assignees: {entries: assignees}}} isFetching={isFetching} {...actions} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, "fc-watchers__cell");
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(assignees.length);
  });

  it('should render rest control when there are more than 7 assignees', function *() {
    const assignees = [
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'},
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'},
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'},
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'},
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'}
    ];

    watchers = shallowRender(
      <Watchers entity={entity} data={{...data, assignees: {entries: assignees}}} isFetching={isFetching} {...actions} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, "fc-watchers__cell");
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(6);
    expect(ShallowTestUtils.findWithClass(watchers, 'fc-watchers__rest-cell')).not.to.be.empty;
    expect(ShallowTestUtils.findWithClass(watchers, 'fc-watchers__rest-block')).not.to.be.empty;
  });

});
