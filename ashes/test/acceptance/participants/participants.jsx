import _ from 'lodash';
import React from 'react';
import * as ShallowTestUtils from 'react-shallow-testutils';
import { groups } from 'paragons/participants';

describe('Watchers', function () {
  const Participants = requireComponent('participants/participants.jsx').WrappedComponent;
  const styles = requireComponent('participants/participants.css', false);

  let watchers;
  const entity = {
    entityType: 'orders',
    entityId: 'BR0001',
  };

  const actions = {
    addParticipants: _.noop,
    removeParticipant: _.noop,
  };

  const props = {
    asyncActions: {},
    entity,
    ...actions,
    group: groups.watchers,
  };

  afterEach(function () {
    if (watchers) {
      watchers.unmount();
      watchers = null;
    }
  });

  it('should render message when watchers are empty', function *() {
    watchers = shallowRender(
      <Participants {...props} participants={[]} />
    );
    const box = ShallowTestUtils.findWithClass(watchers, styles['empty-list']);
    expect(box).not.to.be.empty;
  });

  it('should render watcher cells for each watcher in array', function *() {
    const assignedWatchers = [
      {name: 'Donkey Admin'},
      {name: 'Admin Donkey'}
    ];

    watchers = shallowRender(
      <Participants {...props} participants={assignedWatchers} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, styles.cell);
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(assignedWatchers.length);
  });

  it('should render rest control when there are more than 7 watchers', function *() {
    const assignedWatchers = [
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
      <Participants {...props} participants={assignedWatchers} />
    );
    const cells = ShallowTestUtils.findAllWithClass(watchers, styles.cell);
    expect(cells).not.to.be.empty;
    expect(cells.length).to.be.equal(assignedWatchers.length);
    expect(ShallowTestUtils.findWithClass(watchers, styles['rest-block'])).not.to.be.empty;
  });

});
