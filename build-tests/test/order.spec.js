import testNotes from './test-notes';
import testWatchers from './test-watchers';
import { AdminApi } from '../helpers/Api';
import placeRandomOrder from '../helpers/placeRandomOrder';
import waitFor from '../helpers/waitFor';
import $ from '../payloads';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('Order', function() {
	this.timeout(35000);

	it('[bvt] Can view order details', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const { fullOrder } = await placeRandomOrder();
		const foundOrder = await step.getOrder(api, fullOrder.referenceNumber).then(r => r.result);
		delete fullOrder.fraudScore;
		delete foundOrder.fraudScore;
		expect(foundOrder).to.deep.equal(fullOrder);
	});

	for (const destinationState of $.orderStateTransitions.remorseHold) {
		this.timeout(35000);
		it(`[bvt] Can change order state from "remorseHold" to "${destinationState}"`, async () => {
			const api = new AdminApi;
			await step.loginAsAdmin(api);
			const { fullOrder } = await placeRandomOrder();
			expect(fullOrder.orderState).to.equal('remorseHold');
			const updatedOrder = await step.updateOrder(api, fullOrder.referenceNumber, { state: destinationState });
			expect(updatedOrder.orderState).to.equal(destinationState);
			const foundOrder = await step.getOrder(api, fullOrder.referenceNumber).then(r => r.result);
			expect(foundOrder).to.deep.equal(updatedOrder);
		});
	}

	for (const destinationState of $.orderStateTransitions.fulfillmentStarted) {
		this.timeout(35000);
		it(`[bvt] Can change order state from "fulfillmentStarted" to "${destinationState}"`, async () => {
			const api = new AdminApi;
			await step.loginAsAdmin(api);
			const { fullOrder } = await placeRandomOrder();
			expect(fullOrder.orderState).to.equal('remorseHold');
			await step.updateOrder(api, fullOrder.referenceNumber, { state: 'fulfillmentStarted' });
			const updatedOrder = await step.updateOrder(api, fullOrder.referenceNumber, { state: destinationState });
			expect(updatedOrder.orderState).to.equal(destinationState);
			const foundOrder = await step.getOrder(api, fullOrder.referenceNumber).then(r => r.result);
			expect(foundOrder).to.deep.equal(updatedOrder);
		});
	}

	it('[bvt] Can increase remorse period', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const { fullOrder } = await placeRandomOrder();
		expect(fullOrder.remorsePeriodEnd).to.be.a('date');
		const updatedOrder = await step.increaseRemorsePeriod(api, fullOrder.referenceNumber);
		expect(updatedOrder.remorsePeriodEnd).to.be.a('date');
		const initialRemorsePeriodEndDate = new Date(fullOrder.remorsePeriodEnd);
		const remorsePeriodEndDateAfterIncrease = new Date(updatedOrder.remorsePeriodEnd);
		expect(remorsePeriodEndDateAfterIncrease > initialRemorsePeriodEndDate).to.be.true;
		const foundOrder = await step.getOrder(api, fullOrder.referenceNumber).then(r => r.result);
		expect(foundOrder).to.deep.equal(updatedOrder);
	});

	// this one is broken, stucks on getShipments
	it('[bvt] Can view shipments', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const { fullOrder } = await placeRandomOrder();
		await step.updateOrder(api, fullOrder.referenceNumber, { state: 'fulfillmentStarted' });
		const response = await waitFor(500, 10000,
			() => step.getShipments(api, fullOrder.referenceNumber),
			r => r.shipments && Array.isArray(r.shipments) && r.shipments.length > 0);
		const shipments = response.shipments;
		expect(shipments).to.exist;
		for (const shipment of shipments) {
			expect(shipment.orderRefNum).to.equal(fullOrder.referenceNumber);
			expect(shipment.id).to.be.a('number');
			expect(shipment.referenceNumber).to.be.a('string');
			expect(shipment.state).to.be.a('string');
		}
	});

	testWatchers({
		objectApi: api => api.orders,
		createObject: () => placeRandomOrder().then(r => r.fullOrder),
		selectId: order => order.referenceNumber,
	});

	testNotes({
		objectType: 'order',
		createObject: () => placeRandomOrder().then(r => r.fullOrder),
		selectId: order => order.referenceNumber,
	});
});
