/* @flow */
import React from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import Notes from 'components/notes/notes';

import ContentTypes from 'components/content-types/content-types';
import ContentTypesListPage from 'components/content-types/list-page';
import NewContentType from 'components/content-types/content-types-new';
import ContentType from 'components/content-types/content-type';
// import ContentTypeTransactions from 'components/content-types/transactions';

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const contentTypesRoutes =
    router.read('content-types-base', { path: 'content-types', frn: frn.mkt.contentType }, [
      router.read('content-types-list-page', { component: ContentTypesListPage }, [
        router.read('content-types', { component: ContentTypes, isIndex: true }),
        router.read('content-types-activity-trail', {
          path: 'activity-trail',
          dimension: 'content-type',
          component: ActivityTrailPage,
          frn: frn.activity.contentType,
        }),
      ]),
      router.create('content-types-new', { path: 'new', component: NewContentType }),
      router.read('content-type', { path: ':contentType', component: ContentType }, [
        // router.read('content-type-transactions', {
        //   component: ContentTypeTransactions,
        //   isIndex: true,
        //   frn: frn.mkt.ContentTypeTransaction,
        // }),
        router.read('content-type-notes', {
          path: 'notes',
          component: Notes,
          frn: frn.note.ContentType,
        }),
        router.read('content-type-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.ContentType,
        }),
      ]),
    ]);

  return (
    <div>
      {contentTypesRoutes}
    </div>
  );
};

export default getRoutes;
