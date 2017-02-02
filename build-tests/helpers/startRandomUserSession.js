import faker from 'faker';
import $ from '../payloads';

export default async (api) => {
  const email = faker.internet.email();
  const name = faker.name.firstName();
  const password = faker.internet.password();
  await api.auth.signup(email, name, password);
  await api.auth.login(email, password, $.customerOrg);
  return { email, name, password };
};
