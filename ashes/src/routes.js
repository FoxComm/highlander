import React from 'react';
import { Route, IndexRoute, IndexRedirect } from 'react-router';
import Site from './components/site/site';
import AuthPages from './components/site/auth-pages';
import Home from './components/home/home';
import Notes from './components/notes/notes';
import ActivityTrailPage from './components/activity-trail/activity-trail-page';
import StoreCredits from './components/customers/store-credits/store-credits';
import StoreCreditsTransactions from './components/customers/store-credits/transactions';
import NewStoreCredit from './components/customers/store-credits/new-store-credit';
import CouponsListPage from './components/coupons/list';
import Coupons from './components/coupons/coupons';
import CouponPage from './components/coupons/page';
import CouponForm from './components/coupons/form';
import CouponCodes from './components/coupons/codes';

import PluginsList from './components/plugins/plugins-list';
import Plugin from './components/plugins/plugin';

import Login from './components/auth/login';
import SetPassword from './components/auth/set-password';

import catalogRoutes from './routes/catalog';
import customerRoutes from './routes/customers';
import marketingRoutes from './routes/marketing';
import orderRoutes from './routes/orders';
import userRoutes from './routes/users';

import { getClaims } from 'lib/claims';

// no productions pages, make sure these paths are included in `excludedList` in browserify.js
if (process.env.NODE_ENV != 'production') {
  var StyleGuide = require('./components/style-guide/style-guide').default;
  var StyleGuideGrid = require('./components/style-guide/style-guide-grid').default;
  var StyleGuideButtons = require('./components/style-guide/style-guide-buttons').default;
  var StyleGuideContainers = require('./components/style-guide/style-guide-containers').default;

  var AllActivities = require('./components/activity-trail/all').default;
  var AllNotificationItems = require('components/activity-notifications/all').default;
}

const claims = getClaims();
const routes = (
  <Route path="/">
    <IndexRedirect to="/orders/"/>

    <Route component={AuthPages}>
      <Route name="login" path="login" component={Login}/>
      <Route name="set-password" path="signup" component={SetPassword}/>
    </Route>
    <Route component={Site}>
      <IndexRoute name="home" component={Home}/>
      {orderRoutes(claims)}
      {customerRoutes(claims)}
      {catalogRoutes(claims)}
      {marketingRoutes(claims)}
      {process.env.NODE_ENV != 'production' &&
        <Route name='style-guide' path='style-guide' component={StyleGuide}>
          <IndexRoute name='style-guide-grid' component={StyleGuideGrid}/>
          <Route name='style-guide-buttons' path='buttons' component={StyleGuideButtons}/>
          <Route name='style-guide-containers' path='containers' component={StyleGuideContainers}/>
        </Route>
      }
      {process.env.NODE_ENV != 'production' &&
        <Route name='test' path="_">
          <Route name='test-activities' path='activities' component={AllActivities}/>
          <Route name='test-notifications' path='notifications' component={AllNotificationItems}/>
        </Route>
      }
      {userRoutes(claims)}
      <Route path="plugins" name="plugins-base">
        <IndexRoute name="plugins" component={PluginsList} />
        <Route name="plugin" path=":name" component={Plugin} />
      </Route>
    </Route>
  </Route>
);

export default routes;
