import test from '../../helpers/test';
import testNotes from './testNotes';
import testWatchers from './testWatchers';
import createCreditCard from '../../helpers/createCreditCard';
import placeRandomOrder from '../../helpers/placeRandomOrder';
import { AdminApi, CustomerApi } from '../../helpers/Api';
import isArray from '../../helpers/isArray';
import isString from '../../helpers/isString';
import isNumber from '../../helpers/isNumber';
import $ from '../../payloads';

test('[bvt] Can view cart details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const credentials = $.randomUserCredentials();
  const newCustomer = await adminApi.customers.create(credentials);
  const newCard = await createCreditCard(adminApi, newCustomer.id);
  const newGiftCard = await adminApi.giftCards.create($.randomGiftCardPayload());
  const newPromotion = await adminApi.promotions.create('default', $.randomCreatePromotionPayload());
  const newCoupon = await adminApi.coupons.create('default', $.randomCouponPayload(newPromotion.id));
  const couponCodes = await adminApi.couponCodes.generate(newCoupon.id, $.randomGenerateCouponCodesPayload());
  const couponCode = $.randomArrayElement(couponCodes);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const skuCode = newProduct.skus[0].attributes.code.v;
  const customerApi = new CustomerApi(t);
  await customerApi.auth.login(credentials.email, credentials.password, $.customerOrg);
  await customerApi.cart.get();
  const quantity = $.randomNumber(1, 10);
  await customerApi.cart.addSku(skuCode, quantity);
  await customerApi.cart.setShippingAddress($.randomCreateAddressPayload());
  const shippingMethod = $.randomArrayElement(await customerApi.cart.getShippingMethods());
  await customerApi.cart.chooseShippingMethod(shippingMethod.id);
  await customerApi.cart.addCreditCard(newCard.id);
  const gcPayload = { code: newGiftCard.code, amount: newGiftCard.availableBalance };
  await customerApi.cart.addGiftCard(gcPayload);
  const cart = await customerApi.cart.addCoupon(couponCode).then(r => r.result);
  const foundCart = await adminApi.carts.one(cart.referenceNumber).then(r => r.result);
  t.deepEqual(foundCart, cart);
});

test('[bvt] Can list available shipping methods', async (t) => {
  const customerApi = await CustomerApi.loggedIn(t);
  const cart = await customerApi.cart.get();
  await customerApi.cart.setShippingAddress($.randomCreateAddressPayload());
  const shippingMethodsFromCustomerApi = await customerApi.cart.getShippingMethods();
  const adminApi = await AdminApi.loggedIn(t);
  const shippingMethodsFromAdminApi = await adminApi.carts.getShippingMethods(cart.referenceNumber);
  t.truthy(isArray(shippingMethodsFromAdminApi));
  for (const shippingMethod of shippingMethodsFromAdminApi) {
    t.truthy(isNumber(shippingMethod.id));
    t.truthy(isNumber(shippingMethod.price));
    t.truthy(isString(shippingMethod.name));
    t.truthy(isString(shippingMethod.code));
  }
  t.deepEqual(shippingMethodsFromAdminApi, shippingMethodsFromCustomerApi);
});

test('[bvt] Can update line items', async (t) => {
  const customerApi = await CustomerApi.loggedIn(t);
  const { referenceNumber } = await customerApi.cart.get();
  const adminApi = await AdminApi.loggedIn(t);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const skuCode = newProduct.skus[0].attributes.code.v;
  const payload = $.randomLineItemsPayload([skuCode]);
  const updatedCart = await adminApi.carts.addLineItemQuantities(referenceNumber, payload).then(r => r.result);
  t.truthy(updatedCart.lineItems);
  t.truthy(isArray(updatedCart.lineItems.skus));
  t.is(updatedCart.lineItems.skus.length, 1);
  t.is(updatedCart.lineItems.skus[0].sku, skuCode);
  t.is(updatedCart.lineItems.skus[0].quantity, payload[0].quantity);

  // total price field isn't updated after executing the following line below
  // >>> await adminApi.carts.addLineItemQuantities(referenceNumber, payload).then(r => r.result);
  // 
  // const cart = await customerApi.cart.get();
  // t.deepEqual(cart, updatedCart);
});

test('[bvt] Updating one line item doesn\'t affect others', async (t) => {
  const customerApi = await CustomerApi.loggedIn(t);
  const { referenceNumber } = await customerApi.cart.get();
  const adminApi = await AdminApi.loggedIn(t);
  const productPayload1 = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct1 = await adminApi.products.create('default', productPayload1);
  const skuCode1 = newProduct1.skus[0].attributes.code.v;
  const productPayload2 = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct2 = await adminApi.products.create('default', productPayload2);
  const skuCode2 = newProduct2.skus[0].attributes.code.v;
  const placeLineItemsPayload = $.randomLineItemsPayload([skuCode1, skuCode2]);
  const [sku1Quantity, sku2Quantity] = placeLineItemsPayload.map(item => item.quantity);
  await adminApi.carts.addLineItemQuantities(referenceNumber, placeLineItemsPayload);
  const updateSku1Payload = { sku: skuCode1, quantity: $.randomNumber(1, 10) };
  const updatedCart = await adminApi.carts
    .addLineItemQuantities(referenceNumber, [updateSku1Payload]).then(r => r.result);
  const newSku1Quantity = updatedCart.lineItems.skus.find(item => item.sku === skuCode1).quantity;
  const newSku2Quantity = updatedCart.lineItems.skus.find(item => item.sku === skuCode2).quantity;
  t.is(newSku1Quantity, sku1Quantity + updateSku1Payload.quantity);
  t.is(newSku2Quantity, sku2Quantity);
  
  // total price field isn't updated after executing the following lines below
  // >>> await adminApi.carts.addLineItemQuantities(referenceNumber, placeLineItemsPayload);
  // >>> await adminApi.carts.addLineItemQuantities(referenceNumber, [updateSku1Payload]).then(r => r.result);
  // 
  // const cart = await customerApi.cart.get();
  // t.deepEqual(cart, updatedCart);
});

test('[bvt] Can\'t access the cart once order for it has been placed', async (t) => {
  const { fullOrder } = await placeRandomOrder(t);
  try {
    const adminApi = await AdminApi.loggedIn(t);
    await adminApi.carts.one(fullOrder.referenceNumber);
    t.fail('Accessing cart after placing order should have failed, but it succeeded.');
  } catch (error) {
    if (error && error.response) {
      t.is(error.response.status, 400);
      t.truthy(error.response.clientError);
      t.falsy(error.response.serverError);
    } else {
      throw error;
    }
  }
});

testWatchers({
  objectApi: api => api.carts,
  createObject: async (api) => {
    const customerApi = await CustomerApi.loggedIn(api.testContext);
    return customerApi.cart.get();
  },
  selectId: cart => cart.referenceNumber,
});

testNotes({
  objectType: 'order',
  createObject: async (api) => {
    const customerApi = await CustomerApi.loggedIn(api.testContext);
    return customerApi.cart.get();
  },
  selectId: cart => cart.referenceNumber,
});
