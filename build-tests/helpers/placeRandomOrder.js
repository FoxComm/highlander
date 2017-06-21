import createCreditCard from './createCreditCard';
import waitFor from './waitFor';
import { AdminApi, CustomerApi } from '../helpers/Api';
import $ from '../payloads';

export default async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const credentials = $.randomUserCredentials();
  const newCustomer = await adminApi.customers.create(credentials);
  const newCard = await createCreditCard(adminApi, newCustomer.id);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const skuCode = newProduct.skus[0].attributes.code.v;
  const inventory = await waitFor(500, 10000, () => adminApi.inventories.get(skuCode));
  const stockItemId = inventory.summary.find(item => item.type === 'Sellable').stockItem.id;
  await adminApi.inventories.increment(stockItemId, { qty: 1, status: 'onHand', type: 'Sellable' });
  const customerApi = new CustomerApi(t);
  await customerApi.auth.login(credentials.email, credentials.password, $.customerOrg);
  await customerApi.cart.get();
  await customerApi.cart.addSku(skuCode, 1);
  await customerApi.cart.setShippingAddress($.randomCreateAddressPayload());
  const shippingMethod = $.randomArrayElement(await customerApi.cart.getShippingMethods());
  await customerApi.cart.chooseShippingMethod(shippingMethod.id);
  await customerApi.cart.addCreditCard(newCard.id);
  const fullOrder = await customerApi.cart.checkout();
  return { fullOrder, newCard, newCustomer };
};
