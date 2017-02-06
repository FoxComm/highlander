import $ from '../payloads';

export default async (api) => {
  const { email, name, password } = $.randomUserCredentials();
  await api.auth.signup(email, name, password);
  await api.auth.login(email, password, $.customerOrg);
  return { email, name, password };
};
