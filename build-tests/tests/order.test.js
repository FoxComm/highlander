import test from '../helpers/test';
import testNotes from './testNotes';
import testWatchers from './testWatchers';
import { AdminApi } from '../helpers/Api';
import placeRandomOrder from '../helpers/placeRandomOrder';
import waitFor from '../helpers/waitFor';
import isArray from '../helpers/isArray';
import isString from '../helpers/isString';
import isNumber from '../helpers/isNumber';
import isDate from '../helpers/isDate';
import $ from '../payloads';

test('Can view order details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const { fullOrder } = await placeRandomOrder(t);
  const foundOrder = await adminApi.orders.one(fullOrder.referenceNumber).then(r => r.result);
  delete fullOrder.fraudScore;
  delete foundOrder.fraudScore;
  t.deepEqual(foundOrder, fullOrder);
});

for (const destinationState of $.orderStateTransitions.remorseHold) {
  test(`Can change order state from "remorseHold" to "${destinationState}"`, async (t) => {
    const adminApi = await AdminApi.loggedIn(t);
    const { fullOrder } = await placeRandomOrder(t);
    t.is(fullOrder.orderState, 'remorseHold');
    const updatedOrder = await adminApi.orders.update(fullOrder.referenceNumber, { state: destinationState });
    t.is(updatedOrder.orderState, destinationState);
    const foundOrder = await adminApi.orders.one(fullOrder.referenceNumber).then(r => r.result);
    t.deepEqual(foundOrder, updatedOrder);
  });
}

for (const destinationState of $.orderStateTransitions.fulfillmentStarted) {
  test(`Can change order state from "fulfillmentStarted" to "${destinationState}"`, async (t) => {
    const adminApi = await AdminApi.loggedIn(t);
    const { fullOrder } = await placeRandomOrder(t);
    t.is(fullOrder.orderState, 'remorseHold');
    await adminApi.orders.update(fullOrder.referenceNumber, { state: 'fulfillmentStarted' });
    const updatedOrder = await adminApi.orders.update(fullOrder.referenceNumber, { state: destinationState });
    t.is(updatedOrder.orderState, destinationState);
    const foundOrder = await adminApi.orders.one(fullOrder.referenceNumber).then(r => r.result);
    t.deepEqual(foundOrder, updatedOrder);
  });
}

test('Can increase remorse period', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const { fullOrder } = await placeRandomOrder(t);
  t.truthy(isDate(fullOrder.remorsePeriodEnd));
  const updatedOrder = await adminApi.orders.increaseRemorsePeriod(fullOrder.referenceNumber);
  t.truthy(isDate(updatedOrder.remorsePeriodEnd));
  const initialRemorsePeriodEndDate = new Date(fullOrder.remorsePeriodEnd);
  const remorsePeriodEndDateAfterIncrease = new Date(updatedOrder.remorsePeriodEnd);
  t.truthy(remorsePeriodEndDateAfterIncrease > initialRemorsePeriodEndDate);
  const foundOrder = await adminApi.orders.one(fullOrder.referenceNumber).then(r => r.result);
  t.deepEqual(foundOrder, updatedOrder);
});

// the following test is successful in STAGE-TPG, but not in STAGE where shipstation is not connected
// test('Can view shipments', async (t) => {
//   const adminApi = await AdminApi.loggedIn(t);
//   const { fullOrder } = await placeRandomOrder(t);
//   await adminApi.orders.update(fullOrder.referenceNumber, { state: 'fulfillmentStarted' });
//   const response = await waitFor(500, 10000,
//     () => adminApi.inventories.getShipments(fullOrder.referenceNumber),
//     r => r.shipments && isArray(r.shipments) && r.shipments.length > 0);
//   const shipments = response.shipments;
//   t.truthy(shipments);
//   for (const shipment of shipments) {
//     t.is(shipment.orderRefNum, fullOrder.referenceNumber);
//     t.truthy(isNumber(shipment.id));
//     t.truthy(isString(shipment.referenceNumber));
//     t.truthy(isString(shipment.state));
//   }
// });

testWatchers({
  objectApi: api => api.orders,
  createObject: api => placeRandomOrder(api.testContext).then(r => r.fullOrder),
  selectId: order => order.referenceNumber,
});

testNotes({
  objectType: 'order',
  createObject: api => placeRandomOrder(api.testContext).then(r => r.fullOrder),
  selectId: order => order.referenceNumber,
});
