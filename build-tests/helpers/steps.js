export const signup = allure.createStep('customer sign up', async (api, email, name, password) => (
	await api.auth.signup(email, name, password)
));

export const login = allure.createStep('admin login', async (api, email, password, org) => (
	await api.auth.login(email, password, org)
));

export const logout = allure.createStep('logout', async (api) => (
	await api.auth.logout()
));

export const getAccount = allure.createStep('get account', async (api) => (
	await api.account.get()
));

export const getStoreAdmins = allure.createStep('list all store admins', async (api) => (
    await api.storeAdmins.list()
));

export const getStoreAdmin = allure.createStep('Get single store admin from the list', async (api, storeAdmins) => (
    await api.storeAdmins.one(storeAdmins[0].id)
));

export const createAdminUser = allure.createStep('Create new store admin user', async (api, payload) => (
    await api.storeAdmins.create(payload)
));

export const updateAdminUser = allure.createStep('Change existing admin user details', async (api, adminId, payload) => (
    await api.storeAdmins.update(adminId, payload)
));