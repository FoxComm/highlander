/* @flow */

import React from 'react';

import FoxRouter from 'lib/fox-router';

// no productions pages, make sure these paths are included in `excludedList` in browserify.js
if (process.env.NODE_ENV != 'production') {
  var StyleGuide = require('components/style-guide/style-guide').default;
  var StyleGuideGrid = require('components/style-guide/style-guide-grid').default;
  var StyleGuideButtons = require('components/style-guide/style-guide-buttons').default;
  var StyleGuideContainers = require('components/style-guide/style-guide-containers').default;

  var AllActivities = require('components/activity-trail/all').default;
  var AllNotificationItems = require('components/activity-notifications/all').default;
}

import type { JWT } from 'lib/claims';

const getRoutes = (jwt: JWT) => {
  if (process.env.NODE_ENV == 'production') {
    return <div></div>;
  }

  const router = new FoxRouter(jwt);
  const devRoutes =
    router.read('dev', {}, [
      router.read('style-guide', { path: 'style-guide', component: StyleGuide }, [
        router.read('style-guide-grid', { component: StyleGuideGrid, isIndex: true }),
        router.read('style-guide-buttons', { path: 'buttons', component: StyleGuideButtons }),
        router.read('style-guide-containers', { path: 'containers', component: StyleGuideContainers }),
      ]),
      router.read('test', { path: '_' }, [
        router.read('test-activities', { path: 'activities', component: AllActivities }),
        router.read('test-notifications', { path: 'notifications', component: AllNotificationItems }),
      ]),
    ]);

  return devRoutes;
};

export default getRoutes;
