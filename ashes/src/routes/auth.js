/* @flow */

import FoxRouter from 'lib/fox-router';

import AuthPages from 'components/site/auth-pages';
import Login from 'components/auth/login';
import SetPassword from 'components/auth/set-password';

import type { JWT } from 'lib/claims';

const getRoutes = (jwt: JWT) => {
  const router = new FoxRouter(jwt);
  const authRoutes =
    router.read('auth', { component: AuthPages }, [
      router.create('login', { path: 'login', component: Login }),
      router.create('set-password', { path: 'signup', component: SetPassword }),
    ]);

  return authRoutes;
};

export default getRoutes;
