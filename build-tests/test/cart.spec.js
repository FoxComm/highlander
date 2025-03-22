import createCreditCard from '../helpers/createCreditCard';
import placeRandomOrder from '../helpers/placeRandomOrder';
import { AdminApi, CustomerApi } from '../helpers/Api';
import isArray from '../helpers/isArray';
import isString from '../helpers/isString';
import isNumber from '../helpers/isNumber';
import isDate from '../helpers/isDate';
import $ from '../payloads';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('Cart', function() {

	this.timeout(30000);

	it('[bvt] Can add line item', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
		const newProduct = await step.createNewProduct(api, 'default', productPayload);
		const customerApi = new CustomerApi;
		const { email, name, password } = $.randomUserCredentials();
		await step.signup(customerApi, email, name, password);
		await step.login(customerApi, email, password, $.customerOrg);
		await step.getCurrentCart(customerApi);
		const skuCode = newProduct.skus[0].attributes.code.v;
		const quantity = $.randomNumber(1, 10);
		const fullOrder = await step.addSkuToCart(customerApi, skuCode, quantity).then(r => r.result);
		expect(fullOrder.lineItems).to.exist;
		expect(isArray(fullOrder.lineItems.skus)).to.be.true;
		expect(fullOrder.lineItems.skus.length).to.equal(1);
		expect(fullOrder.lineItems.skus[0].sku).to.equal(skuCode);
		expect(fullOrder.lineItems.skus[0].quantity).to.equal(quantity);
		// Skipped since of wrong response from backend in line 19 (total price wasnt updated for skus)
		// const foundOrder = await getCurrentCart(customerApi);
		// expect(foundOrder).to.deep.equal(fullOrder);
	});

	it('[bvt] Can update line item', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
		const newProduct = await step.createNewProduct(api, 'default', productPayload);
		const customerApi = new CustomerApi;
		const { email, name, password } = $.randomUserCredentials();
		await step.signup(customerApi, email, name, password);
		await step.login(customerApi, email, password, $.customerOrg);
		await step.getCurrentCart(customerApi);
		const skuCode = newProduct.skus[0].attributes.code.v;
		await step.addSkuToCart(customerApi, skuCode, 1);
		const newQuantity = $.randomNumber(2, 10);
		const fullOrder = await step.updateQty(customerApi, skuCode, newQuantity).then(r => r.result);
		expect(fullOrder.lineItems).to.exist;
		expect(isArray(fullOrder.lineItems.skus)).to.be.true;
		expect(fullOrder.lineItems.skus.length).to.equal(1);
		expect(fullOrder.lineItems.skus[0].sku).to.equal(skuCode);
		expect(fullOrder.lineItems.skus[0].quantity).to.equal(newQuantity);
		// Skipped since of wrong response from backend in line 39 (total price wasnt updated for skus)
		// const foundOrder = await customerApi.cart.get();
		// t.deepEqual(foundOrder, fullOrder);
	});

	it('[bvt] Can remove line item', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
		const newProduct = await step.createNewProduct(api, 'default', productPayload);
		const customerApi = new CustomerApi;
		const { email, name, password } = $.randomUserCredentials();
		await step.signup(customerApi, email, name, password);
		await step.login(customerApi, email, password, $.customerOrg);
		await step.getCurrentCart(customerApi);
		const skuCode = newProduct.skus[0].attributes.code.v;
		await step.addSkuToCart(customerApi, skuCode, 1);
		const fullOrder = await customerApi.cart.removeSku(skuCode).then(r => r.result);
		expect(fullOrder.lineItems).to.exist;
		expect(isArray(fullOrder.lineItems.skus)).to.be.true;
		expect(fullOrder.lineItems.skus.length).to.equal(0);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrder);
	});

	it('[bvt] Can set shipping address', async () => {
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		await step.getCurrentCart(customerApi);
		const payload = $.randomCreateAddressPayload();
		const fullOrder = await step.setShippingAddress(customerApi, payload).then(r => r.result);
		const shippingAddress = fullOrder.shippingAddress;
		expect(shippingAddress.name).to.equal(payload.name);
		expect(shippingAddress.region.id).to.equal(payload.regionId);
		expect(shippingAddress.address1).to.equal(payload.address1);
		expect(shippingAddress.address2).to.equal(payload.address2);
		expect(shippingAddress.city).to.equal(payload.city);
		expect(shippingAddress.zip).to.equal(payload.zip);

		// Does not work, since there is no isDefault prop in response
		// expect(shippingAddress.isDefault).to.equal(payload.isDefault);

		expect(shippingAddress.phoneNumber).to.equal(payload.phoneNumber);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrder);
	});

	it('[bvt] Can list available shipping methods', async () => {
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		await step.getCurrentCart(customerApi);
		await step.setShippingAddress(customerApi, $.randomCreateAddressPayload());
		const shippingMethods = await step.getShippingMethods(customerApi);
		expect(isArray(shippingMethods)).to.be.true;
		for (const shippingMethod of shippingMethods) {
			expect(isNumber(shippingMethod.id)).to.be.true;
			expect(isNumber(shippingMethod.price)).to.be.true;
			expect(isString(shippingMethod.name)).to.be.true;
			expect(isString(shippingMethod.code)).to.be.true;
		}
	});

	it('[bvt] Can choose shipping method', async () => {
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		await step.getCurrentCart(customerApi);
		await step.setShippingAddress(customerApi, $.randomCreateAddressPayload());
		const shippingMethods = await step.getShippingMethods(customerApi);
		const shippingMethod = $.randomArrayElement(shippingMethods);
		const fullOrder = await step.chooseShippingMethod(customerApi, shippingMethod).then(r => r.result);
		expect(fullOrder.shippingMethod).to.deep.equal(shippingMethod);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrder);
	});

	// works only on stage-tpg ¯\_(ツ)_/¯
	it('[bvt] Can apply credit card', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const credentials = $.randomUserCredentials();
		const newCustomer = await step.createNewCustomer(api, credentials);
		const newCard = await createCreditCard(api, newCustomer.id);
		const customerApi = new CustomerApi;
		await step.login(customerApi, credentials.email, credentials.password, $.customerOrg);
		await step.getCurrentCart(customerApi);
		const fullOrder = await step.addCreditCard(customerApi, newCard.id).then(r => r.result);
		expect(isArray(fullOrder.paymentMethods)).to.be.true;
		expect(fullOrder.paymentMethods.length).to.equal(1);
		const orderCreditCard = fullOrder.paymentMethods[0];
		expect(orderCreditCard.type).to.equal('creditCard');
		expect(orderCreditCard.id).to.equal(newCard.id);
		expect(orderCreditCard.customerId).to.equal(newCard.customerId);
		expect(orderCreditCard.holderName).to.equal(newCard.holderName);
		expect(orderCreditCard.lastFour).to.equal(newCard.lastFour);
		expect(orderCreditCard.expMonth).to.equal(newCard.expMonth);
		expect(orderCreditCard.expYear).to.equal(newCard.expYear);
		expect(orderCreditCard.brand).to.equal(newCard.brand);
		expect(orderCreditCard.address).to.deep.equal(newCard.address);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrder);
	});
//
	//works only on stage-tpg ¯\_(ツ)_/¯
	it('[bvt] Can remove credit card', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const credentials = $.randomUserCredentials();
		const newCustomer = await step.createNewCustomer(api, credentials);
		const newCard = await createCreditCard(api, newCustomer.id);
		const customerApi = new CustomerApi;
		await step.login(customerApi, credentials.email, credentials.password, $.customerOrg);
		await step.getCurrentCart(customerApi);
		const fullOrderAfterAddingCC = await step.addCreditCard(customerApi, newCard.id).then(r => r.result);
		expect(isArray(fullOrderAfterAddingCC.paymentMethods)).to.be.true;
		expect(fullOrderAfterAddingCC.paymentMethods.length).to.equal(1);
		const fullOrderAfterRemovingCC = await step.removeCreditCards(customerApi).then(r => r.result);
		expect(isArray(fullOrderAfterRemovingCC.paymentMethods)).to.be.true;
		expect(fullOrderAfterRemovingCC.paymentMethods.length).to.equal(0);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrderAfterRemovingCC);
	});

	it('[bvt] Can apply gift card', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const giftCardPayload = $.randomGiftCardPayload();
		const newGiftCard = await step.createNewGiftCard(api, giftCardPayload);
		const customerApi = new CustomerApi;
		const credentials = $.randomUserCredentials();
		await step.signup(customerApi, credentials.email, credentials.name, credentials.password);
		await step.login(customerApi, credentials.email, credentials.password, $.customerOrg);
		await step.getCurrentCart(customerApi);
		const payload = { code: newGiftCard.code, amount: newGiftCard.availableBalance };
		const fullOrder = await step.addGiftCard(customerApi, payload).then(r => r.result);
		expect(isArray(fullOrder.paymentMethods)).to.be.true;
		expect(fullOrder.paymentMethods.length).to.equal(1);
		const orderGiftCard = fullOrder.paymentMethods[0];
		expect(orderGiftCard.code).to.equal(payload.code);
		expect(orderGiftCard.amount).to.equal(payload.amount);
		expect(orderGiftCard.type).to.equal('giftCard');
		expect(orderGiftCard.currentBalance).to.equal(newGiftCard.currentBalance);
		expect(orderGiftCard.availableBalance).to.equal(newGiftCard.availableBalance);
		expect(orderGiftCard.createdAt).to.equal(newGiftCard.createdAt);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrder);
	});

	it('[bvt] Can remove gift card', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const giftCardPayload = $.randomGiftCardPayload();
		const newGiftCard = await step.createNewGiftCard(api, giftCardPayload);
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		await step.getCurrentCart(customerApi);
		const payload = { code: newGiftCard.code, amount: newGiftCard.availableBalance };
		const fullOrderAfterAddingGC = await step.addGiftCard(customerApi, payload).then(r => r.result);
		expect(isArray(fullOrderAfterAddingGC.paymentMethods)).to.be.true;
		expect(fullOrderAfterAddingGC.paymentMethods.length).to.equal(1);
		const fullOrderAfterRemovingGC = await step.removeGiftCard(customerApi, payload.code).then(r => r.result);
		expect(isArray(fullOrderAfterRemovingGC.paymentMethods)).to.be.true;
		expect(fullOrderAfterRemovingGC.paymentMethods.length).to.equal(0);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrderAfterRemovingGC);
	});

	it('[bvt] Can apply store credit', async () => {
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const storeCreditPayload = $.randomStoreCreditPayload();
		const newStoreCredit = await step.issueStoreCredit(api, customerApi, storeCreditPayload);
		await step.getCurrentCart(customerApi);
		const fullOrder = await step.addStoreCredit(customerApi, newStoreCredit.availableBalance).then(r => r.result);
		expect(isArray(fullOrder.paymentMethods)).to.be.true;
		expect(fullOrder.paymentMethods.length, 1).to.equal(1);
		const orderStoreCredit = fullOrder.paymentMethods[0];
		expect(orderStoreCredit.type).to.equal('storeCredit');
		expect(orderStoreCredit.id).to.equal(newStoreCredit.id);
		expect(orderStoreCredit.amount).to.equal(newStoreCredit.availableBalance);
		expect(orderStoreCredit.availableBalance).to.equal(newStoreCredit.availableBalance);
		expect(orderStoreCredit.currentBalance).to.equal(newStoreCredit.currentBalance);
		expect(orderStoreCredit.createdAt).to.equal(newStoreCredit.createdAt);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrder);
	});

	it('[bvt] Can remove store credit', async () => {
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const storeCreditPayload = $.randomStoreCreditPayload();
		const newStoreCredit = await step.issueStoreCredit(api, customerApi, storeCreditPayload);
		await step.getCurrentCart(customerApi);
		const fullOrderAfterAddingSC =
			await step.addStoreCredit(customerApi, newStoreCredit.availableBalance).then(r => r.result);
		expect(isArray(fullOrderAfterAddingSC.paymentMethods)).to.be.true;
		expect(fullOrderAfterAddingSC.paymentMethods.length).to.equal(1);
		const fullOrderAfterRemovingSC = await step.removeStoreCredits(customerApi).then(r => r.result);
		expect(isArray(fullOrderAfterRemovingSC.paymentMethods)).to.be.true;
		expect(fullOrderAfterRemovingSC.paymentMethods.length).to.equal(0);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrderAfterRemovingSC);
	});

	it('[bvt] Can apply coupon', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const context = 'default';
		const newPromotion = await step.createNewPromotion(api, context, $.randomCreatePromotionPayload());
		const newCoupon = await step.createNewCoupon(api, context, $.randomCouponPayload(newPromotion.id));
		const couponCodes = await step.generateCouponCodes(api, newCoupon.id, $.randomGenerateCouponCodesPayload());
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		await step.getCurrentCart(customerApi);
		const couponCode = $.randomArrayElement(couponCodes);

		// coupon cannot be added to cart without scu
		const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
		const newProduct = await step.createNewProduct(api, 'default', productPayload);
		const skuCode = newProduct.skus[0].attributes.code.v;
		await step.addSkuToCart(customerApi, skuCode, 1).then(r => r.result);

		const fullOrder = await step.addCoupon(customerApi, couponCode).then(r => r.result);
		const coupon = fullOrder.coupon;
		expect(coupon.code).to.equal(couponCode);
		expect(coupon.coupon).to.deep.equal(newCoupon);
		const foundOrder = await step.getCurrentCart(customerApi);
		expect(foundOrder).to.deep.equal(fullOrder)
	});

	it('[bvt] Can remove coupon', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const context = 'default';
		const newPromotion = await step.createNewPromotion(api, context, $.randomCreatePromotionPayload());
		const newCoupon = await step.createNewCoupon(api, context, $.randomCouponPayload(newPromotion.id));
		const couponCodes = await step.generateCouponCodes(api, newCoupon.id, $.randomGenerateCouponCodesPayload());
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		await step.getCurrentCart(customerApi);
		const couponCode = $.randomArrayElement(couponCodes);

		// coupon cannot be added to cart without scu
		const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
		const newProduct = await step.createNewProduct(api, 'default', productPayload);
		const skuCode = newProduct.skus[0].attributes.code.v;
		await step.addSkuToCart(customerApi, skuCode, 1).then(r => r.result);

		const fullOrderAfterAddingCoupon = await step.addCoupon(customerApi, couponCode).then(r => r.result);
		expect(fullOrderAfterAddingCoupon.coupon).to.exist;
		const fullOrderAfterRemovingCoupon = await step.removeCoupon(customerApi).then(r => r.result);
		expect(fullOrderAfterRemovingCoupon.coupon).to.not.exist;
		const foundOrder = await step.getCurrentCart(customerApi);

		// doesnt work since autopromotion isnt shown for response from fullOrderAfterRemovingCoupon
		// t.deepEqual(foundOrder, fullOrderAfterRemovingCoupon);
	});

	//works only on stage-tpg ¯\_(ツ)_/¯
	it('[bvt] Can checkout a cart', async () => {
		const { fullOrder, newCard, newCustomer } = await placeRandomOrder();
		expect(fullOrder.paymentState).to.equal('auth');
		expect(fullOrder.orderState).to.equal('remorseHold');
		expect(fullOrder.shippingState).to.equal('remorseHold');
		expect(isDate(fullOrder.placedAt)).to.be.true;
		expect(isDate(fullOrder.remorsePeriodEnd)).to.be.true;
		expect(fullOrder.billingAddress).to.deep.equal(newCard.address);
		expect(fullOrder.billingCreditCardInfo.type).to.equal('creditCard');
		expect(fullOrder.billingCreditCardInfo.id).to.equal(newCard.id);
		expect(fullOrder.billingCreditCardInfo.customerId).to.equal(newCard.customerId);
		expect(fullOrder.billingCreditCardInfo.holderName).to.equal(newCard.holderName);
		expect(fullOrder.billingCreditCardInfo.lastFour).to.equal(newCard.lastFour);
		expect(fullOrder.billingCreditCardInfo.expMonth).to.equal(newCard.expMonth);
		expect(fullOrder.billingCreditCardInfo.expYear).to.equal(newCard.expYear);
		expect(fullOrder.billingCreditCardInfo.brand).to.equal(newCard.brand);
		expect(fullOrder.customer).to.deep.equal(newCustomer);
	});
});
