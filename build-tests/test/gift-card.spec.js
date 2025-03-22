import testNotes from './test-notes';
import { AdminApi, CustomerApi } from '../helpers/Api';
import createCreditCard from '../helpers/createCreditCard';
import waitFor from '../helpers/waitFor';
import isNumber from '../helpers/isNumber';
import isString from '../helpers/isString';
import isDate from '../helpers/isDate';
import $ from '../payloads';
import config from '../config';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('Gift Card', function() {

	this.timeout(30000);

	it('[bvt] Can create a gift card', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomGiftCardPayload();
		const newGiftCard = await step.createNewGiftCard(api, payload);
		expect(isNumber(newGiftCard.id)).to.be.true;
		expect(isDate(newGiftCard.createdAt)).to.be.true;
		expect(isString(newGiftCard.code)).to.be.true;
		expect(newGiftCard.code.length).to.equal(16);
		expect(isNumber(newGiftCard.originId)).to.be.true;
		expect(newGiftCard.originType).to.equal('csrAppeasement');
		expect(newGiftCard.state).to.equal('active');
		expect(newGiftCard.currency).to.equal('USD');
		expect(newGiftCard.originalBalance).to.equal(payload.balance);
		expect(newGiftCard.availableBalance).to.equal(payload.balance);
		expect(newGiftCard.currentBalance).to.equal(payload.balance);
		expect(newGiftCard.storeAdmin.email).to.equal($.adminEmail);
		expect(newGiftCard.storeAdmin.name).to.equal($.adminName);
	});

	it('[bvt] Can view gift card details', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomGiftCardPayload();
		const newGiftCard = await step.createNewGiftCard(api, payload);
		const foundGiftCard = await step.getGiftCard(api, newGiftCard.code);
		expect(foundGiftCard).to.deep.equal(newGiftCard);
	});

	it('[bvt] Can put a gift card "On Hold"', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newGiftCard = await step.createNewGiftCard(api, $.randomGiftCardPayload());
		const updatedGiftCard = await step.updateGiftCard(api, newGiftCard.code, { state: 'onHold' });
		expect(updatedGiftCard.state).to.equal('onHold');
		expect(updatedGiftCard.id).to.equal(newGiftCard.id);
		expect(updatedGiftCard.createdAt).to.equal(newGiftCard.createdAt);
		expect(updatedGiftCard.code).to.equal(newGiftCard.code);
		expect(updatedGiftCard.originId).to.equal(newGiftCard.originId);
		expect(updatedGiftCard.originType).to.equal(newGiftCard.originType);
		expect(updatedGiftCard.currency).to.equal(newGiftCard.currency);
		expect(updatedGiftCard.originalBalance).to.equal(newGiftCard.originalBalance);
		expect(updatedGiftCard.availableBalance).to.equal(newGiftCard.availableBalance);
		expect(updatedGiftCard.currentBalance).to.equal(newGiftCard.currentBalance);
	});

	it('[bvt] Can "Cancel" a gift card', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newGiftCard = await step.createNewGiftCard(api, $.randomGiftCardPayload());
		const payload = { state: 'canceled', reasonId: 1 };
		const updatedGiftCard = await step.updateGiftCard(api, newGiftCard.code, payload);
		expect(updatedGiftCard.state).to.equal(payload.state);
		expect(updatedGiftCard.canceledReason).to.equal(payload.reasonId);
		expect(updatedGiftCard.id).to.equal(newGiftCard.id);
		expect(updatedGiftCard.createdAt).to.equal(newGiftCard.createdAt);
		expect(updatedGiftCard.code).to.equal(newGiftCard.code);
		expect(updatedGiftCard.originId).to.equal(newGiftCard.originId);
		expect(updatedGiftCard.originType).to.equal(newGiftCard.originType);
		expect(updatedGiftCard.currency).to.equal(newGiftCard.currency);
		expect(updatedGiftCard.originalBalance).to.equal(newGiftCard.originalBalance);
		expect(updatedGiftCard.availableBalance).to.equal(newGiftCard.availableBalance);
		expect(updatedGiftCard.currentBalance).to.equal(newGiftCard.currentBalance);
		expect(updatedGiftCard.canceledAmount).to.equal(newGiftCard.currentBalance);
	});

	it('[bvt] Can make gift card "Active"', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newGiftCard = await step.createNewGiftCard(api, $.randomGiftCardPayload());
		const updatedGiftCardOnHold = await step.updateGiftCard(api, newGiftCard.code, { state: 'onHold' });
		expect(updatedGiftCardOnHold.state).to.equal('onHold');
		const updatedActiveGiftCard = await step.updateGiftCard(api, newGiftCard.code, { state: 'active' });
		expect(updatedActiveGiftCard.state).to.equal('active');
	});

	if (config.testGiftCardFlow) {
		it('[bvt] Can send gift card to a customer', async () => {
			const api = new AdminApi;
			await step.loginAsAdmin(api);
			const credentials = $.randomUserCredentials();
			const newCustomer = await step.createNewCustomer(api, credentials);
			const newCard = await createCreditCard(api, newCustomer.id);
			const inventory = await step.getInventorySkuCode(api, $.testGiftCardSkuCode);
			const stockItemId = inventory.summary.find(item => item.type === 'Sellable').stockItem.id;
			await step.incrementInventories(api, stockItemId, { qty: 1, status: 'onHand', type: 'Sellable' });
			const customerApi = new CustomerApi;
			await step.login(customerApi, credentials.email, credentials.password, $.customerOrg);
			await step.getCurrentCart(customerApi);
			const giftCardAttributes = $.randomGiftCardAttributes({ senderName: credentials.name });
			await step.addSkuToCart(customerApi, $.testGiftCardSkuCode, 1, giftCardAttributes);
			await step.setShippingAddress(customerApi, $.randomCreateAddressPayload());
			const shippingMethod = $.randomArrayElement(await step.getShippingMethods(customerApi));
			await step.chooseShippingMethod(customerApi, shippingMethod.id);
			await step.addCreditCard(customerApi, newCard.id);
			const fullOrder = await step.checkout(customerApi);
			await step.updateOrder(api, fullOrder.referenceNumber, { state: 'fulfillmentStarted' });
			await step.updateOrder(api, fullOrder.referenceNumber, { state: 'shipped' });
			const newGiftCardCode = await waitFor(500, 10000, () =>
					step.getOrder(api, fullOrder.referenceNumber)
						.then(r => r.result.lineItems.skus[0].attributes.giftCard.code),
				isString);
			const foundGiftCard = await step.getGiftCard(api, newGiftCardCode);
			expect(foundGiftCard.message).to.equal(giftCardAttributes.giftCard.message);
			expect(foundGiftCard.recipientName).to.equal(giftCardAttributes.giftCard.recipientName);
			expect(foundGiftCard.recipientEmail).to.equal(giftCardAttributes.giftCard.recipientEmail);
			expect(foundGiftCard.senderName).to.equal(giftCardAttributes.giftCard.senderName);
		});
	}

	testNotes({
		objectType: 'gift-card',
		createObject: api => step.createNewGiftCard(api, $.randomGiftCardPayload()),
		selectId: giftCard => giftCard.code,
	});
});
