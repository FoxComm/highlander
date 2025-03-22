import testNotes from './test-notes';
import { AdminApi } from '../helpers/Api';
import isNumber from '../helpers/isNumber';
import isArray from '../helpers/isArray';
import isDate from '../helpers/isDate';
import $ from '../payloads';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('[bvt] Coupon', function() {
	this.timeout(30000);

	it('[bvt] Can create a coupon', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newPromotion = await step.createNewPromotion(api, 'default', $.randomCreatePromotionPayload());
		const payload = $.randomCouponPayload(newPromotion.id)
		const newCoupon = await step.createNewCoupon(api, 'default', payload);

		// activeFrom date cannot be set for coupon due to intentional decision
		payload.attributes.activeFrom.v = newCoupon.attributes.activeFrom.v;

		expect(isNumber(newCoupon.id)).to.be.true;
		expect(newCoupon.promotion).to.equal(newPromotion.id);
		expect(newCoupon.context.name).to.equal('default');
		expect(newCoupon.attributes).to.deep.equal(payload.attributes);
	});

	it('[bvt] Can view coupon details', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newPromotion = await step.createNewPromotion(api, 'default', $.randomCreatePromotionPayload());
		const newCoupon = await step.createNewCoupon(api, 'default', $.randomCouponPayload(newPromotion.id));
		const foundCoupon = await step.getCoupon(api, 'default', newCoupon.id);
		expect(foundCoupon).to.deep.equal(newCoupon);
	});

	it('[bvt] Can update coupon details', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newPromotion = await step.createNewPromotion(api, 'default', $.randomCreatePromotionPayload());
		const newCoupon = await step.createNewCoupon(api, 'default', $.randomCouponPayload(newPromotion.id));
		const payload = $.randomCouponPayload(newPromotion.id);
		const updatedCoupon = await step.updateCoupon(api, 'default', newCoupon.id, payload);

		// activeFrom date cannot be set for coupon due to intentional decision
		payload.attributes.activeFrom.v = newCoupon.attributes.activeFrom.v;

		expect(updatedCoupon.id).to.equal(newCoupon.id);
		expect(updatedCoupon.promotion).to.equal(newPromotion.id);
		expect(updatedCoupon.context.name).to.equal('default');
		expect(updatedCoupon.attributes).to.deep.equal(payload.attributes);
	});

	it('[bvt] Can bulk generate the codes', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newPromotion = await step.createNewPromotion(api, 'default', $.randomCreatePromotionPayload());
		const newCoupon = await step.createNewCoupon(api, 'default', $.randomCouponPayload(newPromotion.id));
		const payload = $.randomGenerateCouponCodesPayload();
		const couponCodes = await step.generateCouponCodes(api, newCoupon.id, payload);
		expect(isArray(couponCodes)).to.be.true;
		expect(couponCodes.length).to.equal(payload.quantity);
		for (const code of couponCodes) {
			expect(code.indexOf(payload.prefix)).to.equal(0);
			expect(code.length).to.equal(payload.length);
		}
	});

	it('[bvt] Can view the list of coupon codes', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newPromotion = await step.createNewPromotion(api, 'default', $.randomCreatePromotionPayload());
		const newCoupon = await step.createNewCoupon(api, 'default', $.randomCouponPayload(newPromotion.id));
		const initialCouponCodes = await step.getCouponCodes(api, newCoupon.id);

		expect(isArray(initialCouponCodes)).to.be.true;
		expect(initialCouponCodes.length).to.equal(0);

		const payload = $.randomGenerateCouponCodesPayload();
		await step.generateCouponCodes(api, newCoupon.id, payload);
		const couponCodesAfterGeneration = await step.getCouponCodes(api, newCoupon.id);

		expect(isArray(couponCodesAfterGeneration)).to.be.true;
		expect(couponCodesAfterGeneration.length).to.equal(payload.quantity);
		for (const { code, createdAt } of couponCodesAfterGeneration) {
			expect(code.indexOf(payload.prefix)).to.equal(0);
			expect(code.length).to.equal(payload.length);
			expect(isDate(createdAt)).to.be.true;
		}
	});

	testNotes({
		objectType: 'coupon',
		createObject: async (api) => {
			const newPromotion = await step.createNewPromotion(api, 'default', $.randomCreatePromotionPayload());
			return step.createNewCoupon(api, 'default', $.randomCouponPayload(newPromotion.id));
		},
	});
});

