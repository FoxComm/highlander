import $ from '../payloads';

//TODO structure steps, to make them more readable

export const signup = allure.createStep('Sign Up', (api, email, name, password) => (
  api.auth.signup(email, name, password)
));

export const login = allure.createStep('Login', (api, email, password, org) => (
	api.auth.login(email, password, org)
));

export const loginAsAdmin = (api) => login(api, $.adminEmail, $.adminPassword, $.adminOrg);

export const logout = allure.createStep('Logout', (api) => (
	api.auth.logout()
));

export const loginAsCustomer = async (api) => {
	const { email, name, password } = $.randomUserCredentials();
	const account = await signup(api, email, name, password);
	await login(api, email, password, $.customerOrg);
	api.account = { id: account.user.id, email, name, password };
	return api;
};

export const getAccount = allure.createStep('Get account', (api) => (
	api.account.get()
));

export const getStoreAdmins = allure.createStep('List all store admins', (api) => (
  api.storeAdmins.list()
));

export const getStoreAdmin = allure.createStep('Get single store admin from the list', (api, storeAdmins) => (
  api.storeAdmins.one(storeAdmins[0].id)
));

export const createAdminUser = allure.createStep('Create new store admin user', (api, payload) => (
  api.storeAdmins.create(payload)
));

export const updateAdminUser = allure.createStep('Change existing admin user details', (api, adminId, payload) => (
  api.storeAdmins.update(adminId, payload)
));

export const createNewCustomer = allure.createStep('Create new customer', (api, credentials) => (
	api.customers.create(credentials)
));

export const getCreditCardToken = allure.createStep('Get credit card token', (api, payload) => (
  api.dev.creditCardToken(payload)
));

export const createNewPromotion = allure.createStep('Create new promotion', (api, context, payload) => (
	api.promotions.create(context, payload)
));

export const createNewCoupon = allure.createStep('Create new coupon', (api, context, payload) => (
	api.coupons.create(context, payload)
));

export const getCoupon = allure.createStep('Get a single coupon', (api, context, couponId) => (
	api.coupons.one(context, couponId)
));

export const updateCoupon = allure.createStep('Update coupon', (api, context, couponId, payload) => (
	api.coupons.update(context, couponId, payload)
));

export const generateCouponCodes = allure.createStep('Generate coupon codes', (api, couponId, payload) => (
	api.couponCodes.generate(couponId, payload)
));

export const getCouponCodes = allure.createStep('List coupon codes', (api, couponId) => (
	api.couponCodes.list(couponId)
));

export const getNotes = allure.createStep('List test notes', (api, objectType, selectId) => (
	api.notes.list(objectType, selectId)
));

export const newNote = allure.createStep('Create new note', (api, objectType, selectId, payload) => (
	api.notes.create(objectType, selectId, payload)
));

export const updateNote = allure.createStep('Update note', (api, objectType, selectId, noteId, payload) => (
	api.notes.update(objectType, selectId, noteId, payload)
));

export const addCustomerCreditCard = allure.createStep('Add customer credit card', (api, customerId, payload) => (
	api.customerCreditCards.add(customerId, payload)
));

export const createNewProduct = allure.createStep('Create new product', (api, context, payload) => (
	api.products.create(context, payload)
));

export const getInventorySkuCode = allure.createStep('Get SKU code from inventory', (api, skuCode) => (
	api.inventories.get(skuCode)
));

export const incrementInventories = allure.createStep('Increment inventories', (api, itemId, payload) => (
	api.inventories.increment(itemId, payload)
));

export const getCurrentCart = allure.createStep('Get current cart', (api) => (
	api.cart.get()
));

export const addSkuToCart = allure.createStep('add SKU to cart', (api, skuCode, quantity, attrs) => (
	api.cart.addSku(skuCode, quantity, attrs)
));

export const setShippingAddress = allure.createStep('set Shipping address to cart', (api, payload) => (
	api.cart.setShippingAddress(payload)
));

export const getShippingMethods = allure.createStep('List shipping methods', (api) => (
	api.cart.getShippingMethods()
));

export const chooseShippingMethod = allure.createStep('Choose shipping method', (api, method) => (
	api.cart.chooseShippingMethod(method.id)
));

export const addCreditCard = allure.createStep('Add credit card', (api, cardId) => (
	api.cart.addCreditCard(cardId)
));

export const checkout = allure.createStep('Checkout', (api) => (
	api.cart.checkout()
));

export const getOrder = allure.createStep('Get a single order', (api, referenceNumber) => (
	api.orders.one(referenceNumber)
));

export const updateOrder = allure.createStep('Update order', (api, referenceNumber, payload) => (
	api.orders.update(referenceNumber, payload)
));

export const increaseRemorsePeriod = allure.createStep('Increase remorse period', (api, referenceNumber) => (
	api.orders.increaseRemorsePeriod(referenceNumber)
));

export const getShipments = allure.createStep('List shipments', (api, referenceNumber) => (
	api.inventories.getShipments(referenceNumber)
));

export const createNewGiftCard = allure.createStep('Create new gift card', (api, payload) => (
	api.giftCards.create(payload)
));

export const getGiftCard = allure.createStep('Get a single gift card', (api, giftCardCode) => (
	api.giftCards.one(giftCardCode)
));

export const updateGiftCard = allure.createStep('Update gift card', (api, giftCardCode, payload) => (
	api.giftCards.update(giftCardCode, payload)
));

export const deleteSharedSearch = allure.createStep('Delete shared search', (api, code) => (
	api.sharedSearches.delete(code)
));

export const listSharedSearch = allure.createStep('List shared search', (api, scope) => (
	api.sharedSearches.list(scope)
));

export const createNewSharedSearch = allure.createStep('Create new shared search', (api, payload) => (
	api.sharedSearches.create(payload)
));

export const getSharedSearch = allure.createStep('Get a single shared search', (api, code) => (
	api.sharedSearches.one(code)
));

export const getAssociates = allure.createStep('Get shared search associates', (api, code) => (
	api.sharedSearches.getAssociates(code)
));

export const addAssociate = allure.createStep('Add shared search associates', (api, code, payload) => (
	api.sharedSearches.addAssociate(code, payload)
));

export const removeAssociate = allure.createStep('Remove shared search associates', (api, code, adminId) => (
	api.sharedSearches.removeAssociate(code, adminId)
));

export const updateQty = allure.createStep('Update quantity in cart', (api, skuCode, newQty) => (
	api.cart.updateQty(skuCode, newQty)
));

export const removeSku = allure.createStep('Remove SKU', (api, skuCode) => (
	api.cart.removeSku(skuCode)
));

export const removeCreditCards = allure.createStep('Remove credit cards', (api, skuCode) => (
	api.cart.removeCreditCards(skuCode)
));

export const addGiftCard = allure.createStep('Add Gift card', (api, payload) => (
	api.cart.addGiftCard(payload)
));

export const removeGiftCard = allure.createStep('Remove Gift card', (api, giftCardCode) => (
  api.cart.removeGiftCard(giftCardCode)
));

export const issueStoreCredit = allure.createStep('Issue store credit', (api, customerApi, payload) => (
	api.customers.issueStoreCredit(customerApi.account.id, payload)
));

export const addStoreCredit = allure.createStep('Add store credit', (api, availableBalance) => (
	api.cart.addStoreCredit(availableBalance)
));

export const removeStoreCredits = allure.createStep('Remove store credit', (api) => (
	api.cart.removeStoreCredits()
));

export const addCoupon = allure.createStep('Add coupon', (api, couponCode) => (
	api.cart.addCoupon(couponCode)
));

export const removeCoupon = allure.createStep('Remove coupon', (api, couponCode) => (
	api.cart.removeCoupon()
));


