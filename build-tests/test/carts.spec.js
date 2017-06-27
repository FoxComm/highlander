import testNotes from './test-notes';
import testWatchers from './test-watchers';
import createCreditCard from '../helpers/createCreditCard';
import placeRandomOrder from '../helpers/placeRandomOrder';
import { AdminApi, CustomerApi } from '../helpers/Api';
import isArray from '../helpers/isArray';
import isString from '../helpers/isString';
import isNumber from '../helpers/isNumber';
import $ from '../payloads';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('Carts', function() {

	this.timeout(30000);

	it('[bvt] Can view cart details', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const credentials = $.randomUserCredentials();
		const newCustomer = await step.createNewCustomer(api, credentials);
		const newCard = await createCreditCard(api, newCustomer.id);
		const newGiftCard = await step.createNewGiftCard(api, $.randomGiftCardPayload());
		const newPromotion = await step.createNewPromotion(api, 'default', $.randomCreatePromotionPayload());
		const context = 'default';
		const newCoupon = await step.createNewCoupon(api, context, $.randomCouponPayload(newPromotion.id));
		const couponCodes = await step.generateCouponCodes(api, newCoupon.id, $.randomGenerateCouponCodesPayload());
		const couponCode = $.randomArrayElement(couponCodes);
		const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
		const newProduct = await step.createNewProduct(api, context, productPayload);
		const skuCode = newProduct.skus[0].attributes.code.v;
		const customerApi = new CustomerApi;
		await step.login(customerApi, credentials.email, credentials.password, $.customerOrg);
		await step.getCurrentCart(customerApi);
		const quantity = $.randomNumber(1, 10);
		await step.addSkuToCart(customerApi, skuCode, quantity);
		await step.setShippingAddress(customerApi, $.randomCreateAddressPayload());
		const shippingMethod = $.randomArrayElement(await step.getShippingMethods(customerApi));
		await step.chooseShippingMethod(customerApi, shippingMethod.id);
		await step.addCreditCard(customerApi, newCard.id);
		const gcPayload = { code: newGiftCard.code, amount: newGiftCard.availableBalance };
		await step.addGiftCard(customerApi, gcPayload);
		const cart = await step.addCoupon(customerApi, couponCode).then(r => r.result);
		const foundCart = await step.getCart(api, cart.referenceNumber).then(r => r.result);
		expect(foundCart).to.deep.equal(cart);
	});

	it('[bvt] Can list available shipping methods', async () => {
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		const cart = await step.getCurrentCart(customerApi);
		await step.setShippingAddress(customerApi, $.randomCreateAddressPayload());
		const shippingMethodsFromCustomerApi = await step.getShippingMethods(customerApi);
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const shippingMethodsFromAdminApi = await step.getCartsShippingMethods(api, cart);
		expect(isArray(shippingMethodsFromAdminApi)).to.be.true;
		for (const shippingMethod of shippingMethodsFromAdminApi) {
			expect(isNumber(shippingMethod.id)).to.be.true;
			expect(isNumber(shippingMethod.price)).to.be.true;
			expect(isString(shippingMethod.name)).to.be.true;
			expect(isString(shippingMethod.code)).to.be.true;
		}
		expect(shippingMethodsFromAdminApi).to.deep.equal(shippingMethodsFromCustomerApi);
	});

	it('[bvt] Can update line items', async () => {
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		const { referenceNumber } = await step.getCurrentCart(customerApi);
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
		const newProduct = await step.createNewProduct(api, 'default', productPayload);
		const skuCode = newProduct.skus[0].attributes.code.v;
		const payload = $.randomLineItemsPayload([skuCode]);
		const updatedCart = await step.addLineItemQuantities(api, referenceNumber, payload).then(r => r.result);
		expect(updatedCart.lineItems).to.exist;
		expect(isArray(updatedCart.lineItems.skus)).to.be.true;
		expect(updatedCart.lineItems.skus.length).to.equal(1);
		expect(updatedCart.lineItems.skus[0].sku).to.equal(skuCode);
		expect(updatedCart.lineItems.skus[0].quantity).to.equal(payload[0].quantity);

		// total price field isn't updated after executing the following line below
		// >>> await adminApi.carts.addLineItemQuantities(referenceNumber, payload).then(r => r.result);
		//
		// const cart = await customerApi.cart.get();
		// t.deepEqual(cart, updatedCart);
	});

	it('[bvt] Updating one line item doesn\'t affect others', async () => {
		const customerApi = new CustomerApi;
		await step.loginAsCustomer(customerApi);
		const { referenceNumber } = await step.getCurrentCart(customerApi);
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const productPayload1 = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
		const newProduct1 = await step.createNewProduct(api, 'default', productPayload1);
		const skuCode1 = newProduct1.skus[0].attributes.code.v;
		const productPayload2 = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
		const newProduct2 = await step.createNewProduct(api, 'default', productPayload2);
		const skuCode2 = newProduct2.skus[0].attributes.code.v;
		const placeLineItemsPayload = $.randomLineItemsPayload([skuCode1, skuCode2]);
		const [sku1Quantity, sku2Quantity] = placeLineItemsPayload.map(item => item.quantity);
		await step.addLineItemQuantities(api, referenceNumber, placeLineItemsPayload);
		const updateSku1Payload = { sku: skuCode1, quantity: $.randomNumber(1, 10) };
		const updatedCart = await step
			.addLineItemQuantities(api, referenceNumber, [updateSku1Payload])
			.then(r => r.result);
		const newSku1Quantity = updatedCart.lineItems.skus.find(item => item.sku === skuCode1).quantity;
		const newSku2Quantity = updatedCart.lineItems.skus.find(item => item.sku === skuCode2).quantity;
		expect(newSku1Quantity).to.equal(sku1Quantity + updateSku1Payload.quantity);
		expect(newSku2Quantity).to.equal(sku2Quantity);

		// total price field isn't updated after executing the following lines below
		// >>> await adminApi.carts.addLineItemQuantities(referenceNumber, placeLineItemsPayload);
		// >>> await adminApi.carts.addLineItemQuantities(referenceNumber, [updateSku1Payload]).then(r => r.result);
		//
		// const cart = await customerApi.cart.get();
		// t.deepEqual(cart, updatedCart);
	});
	TODO: BROKEN
	it('[bvt] Can\'t access the cart once order for it has been placed', async () => {
		const { fullOrder } = await placeRandomOrder();
		try {
			const api = new AdminApi;
			await step.loginAsAdmin(api);
			await step.getCart(api, fullOrder.referenceNumber);
			expect('Accessing cart after placing order should have failed, but it succeeded.').to.fail;
		} catch (error) {
			if (error && error.response) {
				expect(error.response.status).to.equal(400);
				expect(error.response.clientError).to.exist;
				expect(error.response.serverError).to.not.exist;
			} else {
				throw error;
			}
		}
	});

	testWatchers({
		objectApi: api => api.carts,
		createObject: async () => {
			const customerApi = new CustomerApi;
			await step.loginAsCustomer(customerApi);
			return await step.getCurrentCart(customerApi);
		},
		selectId: cart => cart.referenceNumber,
	});

	testNotes({
		objectType: 'order',
		createObject: async (api) => {
			const customerApi = await CustomerApi.loggedIn(api.testContext);
			return await step.getCurrentCart(customerApi);
		},
		selectId: cart => cart.referenceNumber,
	});
});

