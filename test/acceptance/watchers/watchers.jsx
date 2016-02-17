import _ from 'lodash';
import React from 'react';
import ShallowTestUtils from 'react-shallow-testutils';

describe('Watchers', function () {
  const { initialState } = importSource('modules/watchers');
  const Watchers = requireComponent('watchers/watchers.jsx');

  let watchers;
  const entityType = 'order';
  const data = {...initialState};
  const actions = {
    showSelectModal: _.noop,
    hideSelectModal: _.noop,
    toggleListModal: _.noop,
    suggestWatchers: _.noop,
    selectItem: _.noop,
    deselectItem: _.noop,
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
      <Watchers entityType={entityType} data={data} actions={actions} />
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
      <Watchers entityType={entityType} data={{...data, assignees: {entries: assignees}}} actions={actions} />
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
      <Watchers entityType={entityType} data={{...data, assignees: {entries: assignees}}} actions={actions} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, "fc-watchers__cell");
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(6);
    expect(ShallowTestUtils.findWithClass(watchers, 'fc-watchers__rest-cell')).not.to.be.empty;
    expect(ShallowTestUtils.findWithClass(watchers, 'fc-watchers__rest-block')).not.to.be.empty;
  });

});
