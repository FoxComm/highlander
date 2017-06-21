import test from '../helpers/test';
import testNotes from './testNotes';
import { AdminApi } from '../helpers/Api';
import isNumber from '../helpers/isNumber';
import isArray from '../helpers/isArray';
import isDate from '../helpers/isDate';
import $ from '../payloads';

test('Can create a coupon', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newPromotion = await adminApi.promotions.create('default', $.randomCreatePromotionPayload());
  const payload = $.randomCouponPayload(newPromotion.id);
  const newCoupon = await adminApi.coupons.create('default', payload);

  // activeFrom date cannot be set for coupon due to intentional decision 
  payload.attributes.activeFrom.v = newCoupon.attributes.activeFrom.v;
  
  t.truthy(isNumber(newCoupon.id));
  t.is(newCoupon.promotion, newPromotion.id);
  t.is(newCoupon.context.name, 'default');
  t.deepEqual(newCoupon.attributes, payload.attributes);
});

test('Can view coupon details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newPromotion = await adminApi.promotions.create('default', $.randomCreatePromotionPayload());
  const newCoupon = await adminApi.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  const foundCoupon = await adminApi.coupons.one('default', newCoupon.id);
  t.deepEqual(foundCoupon, newCoupon);
});

test('Can update coupon details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newPromotion = await adminApi.promotions.create('default', $.randomCreatePromotionPayload());
  const newCoupon = await adminApi.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  const payload = $.randomCouponPayload(newPromotion.id);
  const updatedCoupon = await adminApi.coupons.update('default', newCoupon.id, payload);

  // activeFrom date cannot be set for coupon due to intentional decision
  payload.attributes.activeFrom.v = newCoupon.attributes.activeFrom.v;

  t.is(updatedCoupon.id, newCoupon.id);
  t.is(updatedCoupon.promotion, newPromotion.id);
  t.is(updatedCoupon.context.name, 'default');  
  t.deepEqual(updatedCoupon.attributes, payload.attributes);
});

test('Can bulk generate the codes', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newPromotion = await adminApi.promotions.create('default', $.randomCreatePromotionPayload());
  const newCoupon = await adminApi.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  const payload = $.randomGenerateCouponCodesPayload();
  const couponCodes = await adminApi.couponCodes.generate(newCoupon.id, payload);
  t.truthy(isArray(couponCodes));
  t.is(couponCodes.length, payload.quantity);
  for (const code of couponCodes) {
    t.is(code.indexOf(payload.prefix), 0);
    t.is(code.length, payload.length);
  }
});

test('Can view the list of coupon codes', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newPromotion = await adminApi.promotions.create('default', $.randomCreatePromotionPayload());
  const newCoupon = await adminApi.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  const initialCouponCodes = await adminApi.couponCodes.list(newCoupon.id);
  t.truthy(isArray(initialCouponCodes));
  t.is(initialCouponCodes.length, 0);
  const payload = $.randomGenerateCouponCodesPayload();
  await adminApi.couponCodes.generate(newCoupon.id, payload);
  const couponCodesAfterGeneration = await adminApi.couponCodes.list(newCoupon.id);
  t.truthy(isArray(couponCodesAfterGeneration));
  t.is(couponCodesAfterGeneration.length, payload.quantity);
  for (const { code, createdAt } of couponCodesAfterGeneration) {
    t.is(code.indexOf(payload.prefix), 0);
    t.is(code.length, payload.length);
    t.truthy(isDate(createdAt));
  }
});

testNotes({
  objectType: 'coupon',
  createObject: async (api) => {
    const newPromotion = await api.promotions.create('default', $.randomCreatePromotionPayload());
    return api.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  },
});
