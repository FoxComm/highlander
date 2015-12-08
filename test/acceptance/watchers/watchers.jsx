import React from 'react';
import ShallowTestUtils from 'react-shallow-testutils';

describe('Watchers', function() {
  const Watchers = requireComponent('watchers/watchers.jsx');

  let watchers;

  afterEach(function() {
    if (watchers) {
      watchers.unmount();
      watchers = null;
    }
  });

  it('should render message when assignees are empty', function *() {
    const assignees = [];

    watchers = shallowRender(
      <Watchers assignees={assignees} />
    );
    const box = ShallowTestUtils.findWithClass(watchers, "fc-watchers__assignees-empty");
    expect(box).not.to.be.empty;
    expect(box, 'to contain', 'Unassigned');
  });

  it('should render message when watchers are empty', function *() {
    const watcherList = [];

    watchers = shallowRender(
      <Watchers watchers={watcherList} />
    );
    const box = ShallowTestUtils.findWithClass(watchers, "fc-watchers__watchers-empty");
    expect(box).not.to.be.empty;
    expect(box, 'to contain', 'Unwatched');
  });

  it('should render assignee cells for each assignee in array', function *() {
    const assignees = [
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'}
    ];

    watchers = shallowRender(
      <Watchers assignees={assignees} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, "fc-watchers__assignee-cell");
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(assignees.length);
  });

  it('should render watcher cells for each watcher in array', function *() {
    const watcherList = [
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'}
    ];

    watchers = shallowRender(
      <Watchers watchers={watcherList} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, "fc-watchers__watcher-cell");
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(watcherList.length);
  });

});
