/* @flow */

import React from 'react';
import { Route, IndexRoute } from 'lib/fox-routes';

import { frn } from 'lib/frn';

import AuthPages from 'components/site/auth-pages';
import Login from 'components/auth/login';
import SetPassword from 'components/auth/set-password';

import type { JWT } from 'lib/claims';

const getRoutes = (jwt: JWT) => {
  return (
    <Route name="auth" component={AuthPages}>
      <Route actions="c" name="login" path="login" component={Login} />
      <Route actions="c" name="set-password" path="signup" component={SetPassword} />
    </Route>
  );
};

export default getRoutes;
