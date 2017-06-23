import $ from '../payloads';

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

export const addSkuToCart = allure.createStep('add SKU to cart', (api, skuCode, amount) => (
	api.cart.addSku(skuCode, amount)
));

export const setShippingAddress = allure.createStep('set Shipping address to cart', (api, payload) => (
	api.cart.setShippingAddress(payload)
));

export const getShippingMethods = allure.createStep('List shipping methods', (api) => (
	api.cart.getShippingMethods()
));

export const chooseShippingMethod = allure.createStep('Choose shipping method', (api, methodId) => (
	api.cart.chooseShippingMethod(methodId)
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
