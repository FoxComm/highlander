
export const signup = allure.createStep('sign up', async (api, email, name, password) => (
  await api.auth.signup(email, name, password)
));

export const login = allure.createStep('login', async (api, email, password, org) => (
	await api.auth.login(email, password, org)
));

export const logout = allure.createStep('logout', async (api) => (
	await api.auth.logout()
));

export const getAccount = allure.createStep('get account', async (api) => (
	await api.account.get()
));
