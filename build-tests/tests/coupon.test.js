import test from '../helpers/test';
import testNotes from './testNotes';
import Api from '../helpers/Api';
import isNumber from '../helpers/isNumber';
import isArray from '../helpers/isArray';
import isDate from '../helpers/isDate';
import $ from '../payloads';

test('Can create a coupon', async (t) => {
  const api = await Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newPromotion = await api.promotions.create('default', $.randomCreatePromotionPayload());
  const payload = $.randomCouponPayload(newPromotion.id);
  const newCoupon = await api.coupons.create('default', payload);
  t.truthy(isNumber(newCoupon.id));
  t.is(newCoupon.promotion, newPromotion.id);
  t.is(newCoupon.context.name, 'default');
  t.deepEqual(newCoupon.attributes, payload.attributes);
});

test('Can view coupon details', async (t) => {
  const api = await Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newPromotion = await api.promotions.create('default', $.randomCreatePromotionPayload());
  const newCoupon = await api.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  const foundCoupon = await api.coupons.one('default', newCoupon.id);
  t.deepEqual(foundCoupon, newCoupon);
});

test('Can update coupon details', async (t) => {
  const api = await Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newPromotion = await api.promotions.create('default', $.randomCreatePromotionPayload());
  const newCoupon = await api.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  const payload = $.randomCouponPayload(newPromotion.id);
  const updatedCoupon = await api.coupons.update('default', newCoupon.id, payload);
  t.is(updatedCoupon.id, newCoupon.id);
  t.is(updatedCoupon.promotion, newPromotion.id);
  t.is(updatedCoupon.context.name, 'default');
  t.deepEqual(updatedCoupon.attributes, payload.attributes);
});

test('Can bulk generate the codes', async (t) => {
  const api = await Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newPromotion = await api.promotions.create('default', $.randomCreatePromotionPayload());
  const newCoupon = await api.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  const payload = $.randomGenerateCouponCodesPayload();
  const couponCodes = await api.couponCodes.generate(newCoupon.id, payload);
  t.truthy(isArray(couponCodes));
  t.is(couponCodes.length, payload.quantity);
  for (const code of couponCodes) {
    t.is(code.indexOf(payload.prefix), 0);
    t.is(code.length, payload.length);
  }
});

test('Can view the list of coupon codes', async (t) => {
  const api = await Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newPromotion = await api.promotions.create('default', $.randomCreatePromotionPayload());
  const newCoupon = await api.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  const initialCouponCodes = await api.couponCodes.list(newCoupon.id);
  t.truthy(isArray(initialCouponCodes));
  t.is(initialCouponCodes.length, 0);
  const payload = $.randomGenerateCouponCodesPayload();
  await api.couponCodes.generate(newCoupon.id, payload);
  const couponCodesAfterGeneration = await api.couponCodes.list(newCoupon.id);
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
