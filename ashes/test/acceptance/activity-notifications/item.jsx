
import _ from 'lodash';

import React from 'react';
import * as ShallowTestUtils from 'react-shallow-testutils';

import activities from '../../fixtures/activity-notifications';

describe('NotificationItem', function () {
  const NotificationItem = requireComponent('activity-notifications/item');

  let notificationItem;

  afterEach(function () {
    if (notificationItem) {
      notificationItem.unmount();
      notificationItem = null;
    }
  });

  context('rendering items from activity trail data:', function () {

    _.each(activities, activity => {

      it(`should be rendered when activity is ${activity.kind}`, function *() {
        notificationItem = shallowRender(
          <NotificationItem item={activity} />
        );
        expect(notificationItem).not.to.be.null;
      });

    });

  });

});
