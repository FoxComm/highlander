/* @flow */

import FoxRouter from 'lib/fox-router';

import AuthPages from 'components/site/auth-pages';
import Login from 'components/auth/login';
import SetPassword from 'components/auth/set-password';
import ResetPassword from 'components/auth/reset-password';
import RestorePassword from 'components/auth/restore-password';

import type { JWT } from 'lib/claims';

const getRoutes = (jwt: JWT) => {
  const router = new FoxRouter(jwt);
  const authRoutes =
    router.read('auth', { component: AuthPages }, [
      router.create('login', { path: 'login', component: Login }),
      router.create('set-password', { path: 'signup', component: SetPassword }),
      // request password reset
      router.create('restore-password', { path: 'restore-password', component: RestorePassword }),
      // create new password after request
      router.create('reset-password', { path: 'reset-password', component: ResetPassword }),
    ]);

  return authRoutes;
};

export default getRoutes;
