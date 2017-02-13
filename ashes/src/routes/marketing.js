/* @flow */
import React, { Component, Element } from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import Notes from 'components/notes/notes';

import GiftCards from 'components/gift-cards/gift-cards';
import GiftCardsListPage from 'components/gift-cards/list-page';
import NewGiftCard from 'components/gift-cards/gift-cards-new';
import GiftCard from 'components/gift-cards/gift-card';
import GiftCardTransactions from 'components/gift-cards/transactions';

import PromotionsListPage from 'components/promotions/list';
import Promotions from 'components/promotions/promotions';
import PromotionPage from 'components/promotions/promotion-page';
import PromotionForm from 'components/promotions/promotion-form';
import PromoCouponsPage from 'components/promotions/promotion-coupons';

import CouponsListPage from 'components/coupons/list';
import Coupons from 'components/coupons/coupons';
import CouponPage from 'components/coupons/page';
import CouponForm from 'components/coupons/form';
import CouponCodes from 'components/coupons/codes';

import type { Claims } from 'lib/claims';

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const giftCardRoutes =
    router.read('gift-cards-base', { path: 'gift-cards', frn: frn.mkt.giftCard }, [
      router.read('gift-cards-list-page', { component: GiftCardsListPage }, [
        router.read('gift-cards', { component: GiftCards, isIndex: true }),
        router.read('gift-cards-activity-trail', {
          path: 'activity-trail',
          dimension: 'gift-card',
          component: ActivityTrailPage,
          frn: frn.activity.giftCard,
        }),
      ]),
      router.create('gift-cards-new', { path: 'new', component: NewGiftCard }),
      router.read('giftcard', { path: ':giftCard', component: GiftCard }, [
        router.read('gift-card-transactions', {
          component: GiftCardTransactions,
          isIndex: true,
          frn: frn.mkt.giftCardTransaction,
        }),
        router.read('gift-card-notes', {
          path: 'notes',
          component: Notes,
          frn: frn.note.giftCard,
        }),
        router.read('gift-card-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.giftCard,
        }),
      ]),
    ]);

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
        router.read('promotion-coupons', {
          path: 'coupons',
          component: PromoCouponsPage
        }),
        router.read('promotion-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.promotion,
        }),
      ]),
    ]);

  const couponRoutes =
    router.read('coupons-base', { path: 'coupons', frn: frn.mkt.coupon }, [
      router.read('coupons-list-page', { component: CouponsListPage }, [
        router.read('coupons', { component: Coupons, isIndex: true }),
        router.read('coupons-activity-trail', {
          path: 'activity-trail',
          dimension: 'coupon',
          component: ActivityTrailPage,
          frn: frn.mkt.coupon,
        }),
      ]),
      router.read('coupon', { path: ':couponId', component: CouponPage }, [
        router.read('coupon-details', { component: CouponForm, isIndex: true }),
        router.read('coupon-codes', { path: 'codes', component: CouponCodes }),
        router.read('coupon-notes', {
          path: 'notes',
          component: Notes,
          frn: frn.note.coupon,
        }),
        router.read('coupon-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.coupon,
        }),
      ]),
    ]);

  return (
    <div>
      {giftCardRoutes}
      {promotionsRoutes}
      {couponRoutes}
    </div>
  );
};

export default getRoutes;
