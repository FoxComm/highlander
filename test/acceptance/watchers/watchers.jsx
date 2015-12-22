import _ from 'lodash';
import React from 'react';
import ShallowTestUtils from 'react-shallow-testutils';

describe('Watchers', function() {
  const WatchersComponent = requireComponent('watchers/watchers.jsx');
  const Watchers = WatchersComponent.WrappedComponent;

  let watchers;
  const entity = {entityType: 'order', enitityId: 'ABC123-13'};

  afterEach(function() {
    if (watchers) {
      watchers.unmount();
      watchers = null;
    }
  });

  it('should render message when assignees are empty', function *() {
    watchers = shallowRender(
      <Watchers entity={entity} />
    );
    const box = ShallowTestUtils.findWithClass(watchers, "fc-watchers__assignees-empty");
    expect(box).not.to.be.empty;
    expect(box, 'to contain', 'Unassigned');
  });

  it('should render message when watchers are empty', function *() {
    watchers = shallowRender(
      <Watchers entity={entity} />
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

    const assigneesData = {assignees: {entries: assignees}};

    watchers = shallowRender(
      <Watchers entity={entity} data={assigneesData} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, "fc-watchers__cell");
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(assignees.length);
  });

  it('should render watcher cells for each watcher in array', function *() {
    const watcherList = [
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'}
    ];

    const watchersData = {watchers: {entries: watcherList}};

    watchers = shallowRender(
      <Watchers entity={entity} data={watchersData} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, "fc-watchers__cell");
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(watcherList.length);
  });

  it('should render rest controll when there are more than 7 watchers', function *() {
    const watcherList = [
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

    const watchersData = {watchers: {entries: watcherList}};

    watchers = shallowRender(
      <Watchers entity={entity} data={watchersData} removeFromGroup={_.noop}/>
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, "fc-watchers__cell");
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(watcherList.length);
    expect(ShallowTestUtils.findWithClass(watchers, 'fc-watchers__rest-cell')).not.to.be.empty;
    expect(ShallowTestUtils.findWithClass(watchers, 'fc-watchers__rest-block')).not.to.be.empty;
  });

  it('should render rest controll when there are more than 7 watchers', function *() {
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

    const assigneesData = {assignees: {entries: assignees}};

    watchers = shallowRender(
      <Watchers entity={entity} data={assigneesData} removeFromGroup={_.noop} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, "fc-watchers__cell");
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(assignees.length);
    expect(ShallowTestUtils.findWithClass(watchers, 'fc-watchers__rest-cell')).not.to.be.empty;
    expect(ShallowTestUtils.findWithClass(watchers, 'fc-watchers__rest-block')).not.to.be.empty;
  });

});
