import createCreditCard from './createCreditCard';
import waitFor from './waitFor';
import { AdminApi, CustomerApi } from '../helpers/Api';
import $ from '../payloads';
import * as step from '../helpers/steps';

export default async () => {
	const api = new AdminApi;
	await step.loginAsAdmin(api);
  const credentials = $.randomUserCredentials();
  const newCustomer = await step.createNewCustomer(api, credentials);
  const newCard = await createCreditCard(api, newCustomer.id);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await step.createNewProduct(api, 'default', productPayload);
  const skuCode = newProduct.skus[0].attributes.code.v;
  const inventory = await waitFor(500, 10000, () => step.getInventorySkuCode(api, skuCode));
  const stockItemId = inventory.summary.find(item => item.type === 'Sellable').stockItem.id;
  await step.incrementInventories(api, stockItemId, { qty: 1, status: 'onHand', type: 'Sellable' });
  const customerApi = new CustomerApi;
  await step.login(customerApi, credentials.email, credentials.password, $.customerOrg);
  await step.getCurrentCart(customerApi);
  await step.addSkuToCart(customerApi, skuCode, 1);
  await step.setShippingAddress(customerApi, $.randomCreateAddressPayload());
  const shippingMethod = $.randomArrayElement(await step.getShippingMethods(customerApi));
  await step.chooseShippingMethod(customerApi, shippingMethod.id);
  await step.addCreditCard(customerApi, newCard.id);
  const fullOrder = await step.checkout(customerApi);
  return { fullOrder, newCard, newCustomer };
};
