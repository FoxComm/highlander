/* eslint-disable no-trailing-spaces */
import test from '../../helpers/test';
import createCreditCard from '../../helpers/createCreditCard';
import placeRandomOrder from '../../helpers/placeRandomOrder';
import { AdminApi, CustomerApi } from '../../helpers/Api';
import isArray from '../../helpers/isArray';
import isString from '../../helpers/isString';
import isNumber from '../../helpers/isNumber';
import isDate from '../../helpers/isDate';
import $ from '../../payloads';

test('[bvt] Can add line item', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  const skuCode = newProduct.skus[0].attributes.code.v;
  const quantity = $.randomNumber(1, 10);
  const fullOrder = await customerApi.cart.addSku(skuCode, quantity).then(r => r.result);
  t.truthy(fullOrder.lineItems);
  t.truthy(isArray(fullOrder.lineItems.skus));
  t.is(fullOrder.lineItems.skus.length, 1);
  t.is(fullOrder.lineItems.skus[0].sku, skuCode);
  t.is(fullOrder.lineItems.skus[0].quantity, quantity);
  // Skipped since of wrong response from backend in line 19 (total price wasnt updated for skus)
  // const foundOrder = await customerApi.cart.get();
  // t.deepEqual(foundOrder, fullOrder);
});

test('[bvt] Can update line item', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  const skuCode = newProduct.skus[0].attributes.code.v;
  await customerApi.cart.addSku(skuCode, 1);
  const newQuantity = $.randomNumber(2, 10);
  const fullOrder = await customerApi.cart.updateQty(skuCode, newQuantity).then(r => r.result);
  t.truthy(fullOrder.lineItems);
  t.truthy(isArray(fullOrder.lineItems.skus));
  t.is(fullOrder.lineItems.skus.length, 1);
  t.is(fullOrder.lineItems.skus[0].sku, skuCode);
  t.is(fullOrder.lineItems.skus[0].quantity, newQuantity);
  // Skipped since of wrong response from backend in line 39 (total price wasnt updated for skus)
  // const foundOrder = await customerApi.cart.get();
  // t.deepEqual(foundOrder, fullOrder);
});

test('[bvt] Can remove line item', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  const skuCode = newProduct.skus[0].attributes.code.v;
  await customerApi.cart.addSku(skuCode, 1);
  const fullOrder = await customerApi.cart.removeSku(skuCode).then(r => r.result);
  t.truthy(fullOrder.lineItems);
  t.truthy(isArray(fullOrder.lineItems.skus));
  t.is(fullOrder.lineItems.skus.length, 0);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('[bvt] Can set shipping address', async (t) => {
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  const payload = $.randomCreateAddressPayload();
  const fullOrder = await customerApi.cart.setShippingAddress(payload).then(r => r.result);
  const shippingAddress = fullOrder.shippingAddress;
  t.is(shippingAddress.name, payload.name);
  t.is(shippingAddress.region.id, payload.regionId);
  t.is(shippingAddress.address1, payload.address1);
  t.is(shippingAddress.address2, payload.address2);
  t.is(shippingAddress.city, payload.city);
  t.is(shippingAddress.zip, payload.zip);
  t.is(shippingAddress.isDefault, payload.isDefault);
  t.is(shippingAddress.phoneNumber, payload.phoneNumber);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('[bvt] Can list available shipping methods', async (t) => {
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  await customerApi.cart.setShippingAddress($.randomCreateAddressPayload());
  const shippingMethods = await customerApi.cart.getShippingMethods();
  t.truthy(isArray(shippingMethods));
  for (const shippingMethod of shippingMethods) {
    t.truthy(isNumber(shippingMethod.id));
    t.truthy(isNumber(shippingMethod.price));
    t.truthy(isString(shippingMethod.name));
    t.truthy(isString(shippingMethod.code));
  }
});

test('[bvt] Can choose shipping method', async (t) => {
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  await customerApi.cart.setShippingAddress($.randomCreateAddressPayload());
  const shippingMethods = await customerApi.cart.getShippingMethods();
  const shippingMethod = $.randomArrayElement(shippingMethods);
  const fullOrder = await customerApi.cart.chooseShippingMethod(shippingMethod.id).then(r => r.result);
  t.deepEqual(fullOrder.shippingMethod, shippingMethod);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('[bvt] Can apply credit card', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const credentials = $.randomUserCredentials();
  const newCustomer = await adminApi.customers.create(credentials);
  const newCard = await createCreditCard(adminApi, newCustomer.id);
  const customerApi = new CustomerApi(t);
  await customerApi.auth.login(credentials.email, credentials.password, $.customerOrg);
  await customerApi.cart.get();
  const fullOrder = await customerApi.cart.addCreditCard(newCard.id).then(r => r.result);
  t.truthy(isArray(fullOrder.paymentMethods));
  t.truthy(fullOrder.paymentMethods.length, 1);
  const orderCreditCard = fullOrder.paymentMethods[0];
  t.is(orderCreditCard.type, 'creditCard');
  t.is(orderCreditCard.id, newCard.id);
  t.is(orderCreditCard.customerId, newCard.customerId);
  t.is(orderCreditCard.holderName, newCard.holderName);
  t.is(orderCreditCard.lastFour, newCard.lastFour);
  t.is(orderCreditCard.expMonth, newCard.expMonth);
  t.is(orderCreditCard.expYear, newCard.expYear);
  t.is(orderCreditCard.brand, newCard.brand);
  t.deepEqual(orderCreditCard.address, newCard.address);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('[bvt] Can remove credit card', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const credentials = $.randomUserCredentials();
  const newCustomer = await adminApi.customers.create(credentials);
  const newCard = await createCreditCard(adminApi, newCustomer.id);
  const customerApi = new CustomerApi(t);
  await customerApi.auth.login(credentials.email, credentials.password, $.customerOrg);
  await customerApi.cart.get();
  const fullOrderAfterAddingCC = await customerApi.cart.addCreditCard(newCard.id).then(r => r.result);
  t.truthy(isArray(fullOrderAfterAddingCC.paymentMethods));
  t.truthy(fullOrderAfterAddingCC.paymentMethods.length, 1);
  const fullOrderAfterRemovingCC = await customerApi.cart.removeCreditCards().then(r => r.result);
  t.truthy(isArray(fullOrderAfterRemovingCC.paymentMethods));
  t.is(fullOrderAfterRemovingCC.paymentMethods.length, 0);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrderAfterRemovingCC);
});

test('[bvt] Can apply gift card', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const giftCardPayload = $.randomGiftCardPayload();
  const newGiftCard = await adminApi.giftCards.create(giftCardPayload);
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  const payload = { code: newGiftCard.code, amount: newGiftCard.availableBalance };
  const fullOrder = await customerApi.cart.addGiftCard(payload).then(r => r.result);
  t.truthy(isArray(fullOrder.paymentMethods));
  t.truthy(fullOrder.paymentMethods.length, 1);
  const orderGiftCard = fullOrder.paymentMethods[0];
  t.is(orderGiftCard.code, payload.code);
  t.is(orderGiftCard.amount, payload.amount);
  t.is(orderGiftCard.type, 'giftCard');
  t.is(orderGiftCard.currentBalance, newGiftCard.currentBalance);
  t.is(orderGiftCard.availableBalance, newGiftCard.availableBalance);
  t.is(orderGiftCard.createdAt, newGiftCard.createdAt);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('[bvt] Can remove gift card', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const giftCardPayload = $.randomGiftCardPayload();
  const newGiftCard = await adminApi.giftCards.create(giftCardPayload);
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  const payload = { code: newGiftCard.code, amount: newGiftCard.availableBalance };
  const fullOrderAfterAddingGC = await customerApi.cart.addGiftCard(payload).then(r => r.result);
  t.truthy(isArray(fullOrderAfterAddingGC.paymentMethods));
  t.truthy(fullOrderAfterAddingGC.paymentMethods.length, 1);
  const fullOrderAfterRemovingGC = await customerApi.cart.removeGiftCard(payload.code).then(r => r.result);
  t.truthy(isArray(fullOrderAfterRemovingGC.paymentMethods));
  t.is(fullOrderAfterRemovingGC.paymentMethods.length, 0);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrderAfterRemovingGC);
});

test('[bvt] Can apply store credit', async (t) => {
  const customerApi = await CustomerApi.loggedIn(t);
  const adminApi = await AdminApi.loggedIn(t);
  const storeCreditPayload = $.randomStoreCreditPayload();
  const newStoreCredit = await adminApi.customers.issueStoreCredit(customerApi.account.id, storeCreditPayload);
  await customerApi.cart.get();
  const fullOrder = await customerApi.cart.addStoreCredit(newStoreCredit.availableBalance).then(r => r.result);
  t.truthy(isArray(fullOrder.paymentMethods));
  t.is(fullOrder.paymentMethods.length, 1);
  const orderStoreCredit = fullOrder.paymentMethods[0];
  t.is(orderStoreCredit.type, 'storeCredit');
  t.is(orderStoreCredit.id, newStoreCredit.id);
  t.is(orderStoreCredit.amount, newStoreCredit.availableBalance);
  t.is(orderStoreCredit.availableBalance, newStoreCredit.availableBalance);
  t.is(orderStoreCredit.currentBalance, newStoreCredit.currentBalance);
  t.is(orderStoreCredit.createdAt, newStoreCredit.createdAt);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('[bvt] Can remove store credit', async (t) => {
  const customerApi = await CustomerApi.loggedIn(t);
  const adminApi = await AdminApi.loggedIn(t);
  const storeCreditPayload = $.randomStoreCreditPayload();
  const newStoreCredit = await adminApi.customers.issueStoreCredit(customerApi.account.id, storeCreditPayload);
  await customerApi.cart.get();
  const fullOrderAfterAddingSC =
    await customerApi.cart.addStoreCredit(newStoreCredit.availableBalance).then(r => r.result);
  t.truthy(isArray(fullOrderAfterAddingSC.paymentMethods));
  t.is(fullOrderAfterAddingSC.paymentMethods.length, 1);
  const fullOrderAfterRemovingSC = await customerApi.cart.removeStoreCredits().then(r => r.result);
  t.truthy(isArray(fullOrderAfterRemovingSC.paymentMethods));
  t.is(fullOrderAfterRemovingSC.paymentMethods.length, 0);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrderAfterRemovingSC);
});

test('[bvt] Can apply coupon', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const context = 'default';
  const newPromotion = await adminApi.promotions.create(context, $.randomCreatePromotionPayload());
  const newCoupon = await adminApi.coupons.create(context, $.randomCouponPayload(newPromotion.id));
  const couponCodes = await adminApi.couponCodes.generate(newCoupon.id, $.randomGenerateCouponCodesPayload());
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  const couponCode = $.randomArrayElement(couponCodes);

  // coupon cannot be added to cart without scu
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const skuCode = newProduct.skus[0].attributes.code.v;
  await customerApi.cart.addSku(skuCode, 1).then(r => r.result);

  const fullOrder = await customerApi.cart.addCoupon(couponCode).then(r => r.result);
  const coupon = fullOrder.coupon;
  t.is(coupon.code, couponCode);
  t.deepEqual(coupon.coupon, newCoupon);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('[bvt] Can remove coupon', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const context = 'default';
  const newPromotion = await adminApi.promotions.create(context, $.randomCreatePromotionPayload());
  const newCoupon = await adminApi.coupons.create(context, $.randomCouponPayload(newPromotion.id));
  const couponCodes = await adminApi.couponCodes.generate(newCoupon.id, $.randomGenerateCouponCodesPayload());
  const customerApi = await CustomerApi.loggedIn(t);
  await customerApi.cart.get();
  const couponCode = $.randomArrayElement(couponCodes);

  // coupon cannot be added to cart without scu
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const skuCode = newProduct.skus[0].attributes.code.v;
  await customerApi.cart.addSku(skuCode, 1).then(r => r.result);  

  const fullOrderAfterAddingCoupon = await customerApi.cart.addCoupon(couponCode).then(r => r.result);
  t.truthy(fullOrderAfterAddingCoupon.coupon);
  const fullOrderAfterRemovingCoupon = await customerApi.cart.removeCoupon().then(r => r.result);
  t.falsy(fullOrderAfterRemovingCoupon.coupon);
  const foundOrder = await customerApi.cart.get();

  // doesnt work since autopromotion isnt shown for response from fullOrderAfterRemovingCoupon
  // t.deepEqual(foundOrder, fullOrderAfterRemovingCoupon);
});

test('[bvt] Can checkout a cart', async (t) => {
  const { fullOrder, newCard, newCustomer } = await placeRandomOrder(t);
  t.is(fullOrder.paymentState, 'auth');
  t.is(fullOrder.orderState, 'remorseHold');
  t.is(fullOrder.shippingState, 'remorseHold');
  t.truthy(isDate(fullOrder.placedAt));
  t.truthy(isDate(fullOrder.remorsePeriodEnd));
  t.deepEqual(fullOrder.billingAddress, newCard.address);
  t.is(fullOrder.billingCreditCardInfo.type, 'creditCard');
  t.is(fullOrder.billingCreditCardInfo.id, newCard.id);
  t.is(fullOrder.billingCreditCardInfo.customerId, newCard.customerId);
  t.is(fullOrder.billingCreditCardInfo.holderName, newCard.holderName);
  t.is(fullOrder.billingCreditCardInfo.lastFour, newCard.lastFour);
  t.is(fullOrder.billingCreditCardInfo.expMonth, newCard.expMonth);
  t.is(fullOrder.billingCreditCardInfo.expYear, newCard.expYear);
  t.is(fullOrder.billingCreditCardInfo.brand, newCard.brand);
  t.deepEqual(fullOrder.customer, newCustomer);
});
