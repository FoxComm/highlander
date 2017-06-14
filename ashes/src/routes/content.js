/* @flow */
import React from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import Notes from 'components/notes/notes';

import ContentTypesListPage from 'components/content-types/list';
import ContentTypes from 'components/content-types/content-types';
import ContentTypePage from 'components/content-types/content-type-page';
import ContentTypeForm from 'components/content-types/content-type-form';
import PromoCouponsPage from 'components/content-types/content-type-coupons';
import PromoCouponNewModal from '../components/content-types/content-type-coupon-modal-new';

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const contentRoutes =
    router.read('content-types-base', { path: 'content-types', frn: frn.mkt.promotion }, [
      router.read('content-types-list-page', { component: ContentTypesListPage }, [
        router.read('content-types', { component: ContentTypes, isIndex: true }),
        router.read('content-types-activity-trail', {
          path: 'activity-trail',
          dimension: 'content-type',
          component: ActivityTrailPage,
          frn: frn.mkt.promotion,
        }),
      ]),
      router.read('content-type', { path: ':contentTypeId', component: ContentTypePage }, [
        router.read('content-type-details', { component: ContentTypeForm, isIndex: true }),
        router.read('content-type-notes', {
          path: 'notes',
          component: Notes,
          frn: frn.note.promotion,
        }),
        router.read('content-type-coupons', { path: 'coupons', component: PromoCouponsPage }, [
          router.read('content-type-coupon-new', { path: 'new', component: PromoCouponNewModal })
        ]),
        router.read('content-type-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.promotion,
        }),
      ]),
    ]);

  return (
    <div>
      {contentRoutes}
    </div>
  );
};

export default getRoutes;
