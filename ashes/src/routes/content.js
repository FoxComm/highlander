/* @flow */
import React from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import Notes from 'components/notes/notes';

import PromotionsListPage from 'components/promotions/list';
import Promotions from 'components/promotions/promotions';
import PromotionPage from 'components/promotions/promotion-page';
import PromotionForm from 'components/promotions/promotion-form';
import PromoCouponsPage from 'components/promotions/promotion-coupons';
import PromoCouponNewModal from 'components/promotions/promotion-coupon-modal-new';

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const promotionsRoutes =
    router.read('promotions-base', { path: 'promotions', frn: frn.mkt.promotion }, [
      router.read('promotions-list-page', { component: PromotionsListPage }, [
        router.read('promotions', { component: Promotions, isIndex: true }),
        router.read('promotions-activity-trail', {
          path: 'activity-trail',
          dimension: 'promotion',
          component: ActivityTrailPage,
          frn: frn.mkt.promotion,
        }),
      ]),
      router.read('promotion', { path: ':promotionId', component: PromotionPage }, [
        router.read('promotion-details', { component: PromotionForm, isIndex: true }),
        router.read('promotion-notes', {
          path: 'notes',
          component: Notes,
          frn: frn.note.promotion,
        }),
        router.read('promotion-coupons', { path: 'coupons', component: PromoCouponsPage }, [
          router.read('promotion-coupon-new', { path: 'new', component: PromoCouponNewModal })
        ]),
        router.read('promotion-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.promotion,
        }),
      ]),
    ]);

  return (
    <div>
      {promotionsRoutes}
    </div>
  );
};

export default getRoutes;
