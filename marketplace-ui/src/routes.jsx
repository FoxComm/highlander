import React from 'react';
import { Route, IndexRoute, IndexRedirect } from 'react-router';

import Site from './components/site/site';
import ApplicationPage from './pages/application/application-page';
import AccountPage from './pages/account/account-page';
import InfoPage from './pages/info/info-page';
import Main from './pages/container/main';

const routes = (
  <Route path="/" component={Site}>
    <IndexRedirect to="application" />
    <Route path="application" component={Main}>
      <IndexRoute component={ApplicationPage} />
      <Route path=":ref" component={ApplicationPage} />
      <Route path=":ref/account" component={AccountPage} />
      <Route path=":ref/info" component={InfoPage} />
    </Route>
  </Route>
);

export default routes;
