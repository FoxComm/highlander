import $ from '../payloads';

export default async (api) => {
  const { email, name, password } = $.randomUserCredentials();
  const account = await api.auth.signup(email, name, password);
  await api.auth.login(email, password, $.customerOrg);
  return {
    id: account.user.id,
    email,
    name,
    password,
  };
};
