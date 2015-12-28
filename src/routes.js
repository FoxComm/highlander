import React from 'react';
import { Route, IndexRoute } from 'react-router';
import Site from './components/site/site';
import Home from './components/home/home';
import Rmas from './components/rmas/rmas';
import Rma from './components/rmas/rma';
import RmaChildList from './components/rmas/child-list';
import RmaDetails from './components/rmas/details';
import Orders from './components/orders/orders';
import Order from './components/orders/order';
import OrderDetails from './components/orders/details';
import Customers from './components/customers/customers';
import NewCustomer from './components/customers/new-customer';
import NewDynamicGroup from './components/customers-groups/new-dynamic-group';
import NewManualGroup from './components/customers-groups/new-manual-group';
import Customer from './components/customers/customer';
import CustomerDetails from './components/customers/details';
import Notes from './components/notes/notes';
import Notifications from './components/notifications/notifications';
import ActivityTrailPage from './components/activity-trail/activity-trail-page';
import GiftCards from './components/gift-cards/gift-cards';
import NewGiftCard from './components/gift-cards/gift-cards-new';
import GiftCard from './components/gift-cards/gift-card';
import GiftCardTransactions from './components/gift-cards/transactions';
import StoreCredits from './components/customers/store-credits/store-credits';
import StoreCreditsTransactions from './components/customers/store-credits/transactions';
import NewStoreCredit from './components/customers/store-credits/new-store-credit';

import StyleGuide from './components/style-guide/style-guide';
import StyleGuideGrid from './components/style-guide/style-guide-grid';
import StyleGuideButtons from './components/style-guide/style-guide-buttons';
import StyleGuideContainers from './components/style-guide/style-guide-containers';

const routes = (
  <Route path="/" component={Site}>
    <IndexRoute name="home" component={Home}/>
    <Route name='rmas-base' path='returns'>
      <IndexRoute name='rmas' component={Rmas}/>
      <Route name='rma' path=':rma' component={Rma}>
        <IndexRoute name='rma-details' component={RmaDetails}/>
        <Route name='rma-notes' path='notes' component={Notes}/>
        <Route name='rma-notifications' path='notifications' component={Notifications}/>
        <Route name='rma-activity-trail' path='activity-trail' component={ActivityTrailPage}/>
      </Route>
    </Route>
    <Route name='orders-base' path="orders">
      <IndexRoute name='orders' component={Orders}/>
      <Route name='order' path=':order' component={Order}>
        <IndexRoute name='order-details' component={OrderDetails}/>
        <Route name='order-notes' path='notes' component={Notes}/>
        <Route name='order-returns' path='returns' component={RmaChildList}/>
        <Route name='order-notifications' path='notifications' component={Notifications}/>
        <Route name='order-activity-trail' path='activity-trail' component={ActivityTrailPage}/>
      </Route>
    </Route>
    <Route name='customers-base' path='customers'>
      <IndexRoute name='customers' component={Customers}/>
      <Route name='customers-new' path='new' component={NewCustomer} />
      <Route name='customer' path=':customerId' component={Customer}>
        <IndexRoute name='customer-details' component={CustomerDetails}/>
        <Route name='customer-returns' path='returns' component={RmaChildList}/>
        <Route name='customer-notes' path='notes' component={Notes} />
        <Route name='customer-storecredits' path='storecredit' component={StoreCredits}/>
        <Route name='customer-storecredits-new'
               path='storecredits/new'
               component={NewStoreCredit} />
        <Route name='customer-storecredit-transactions'
               path='storecredits/transactions'
               component={StoreCreditsTransactions} />
      </Route>
      <Route name='groups-new-dynamic' path='groups/new-dynamic' component={NewDynamicGroup} />
      <Route name='groups-new-manual' path='groups/new-manual' component={NewManualGroup} />
    </Route>
    <Route name='gift-cards-base' path='gift-cards'>
      <IndexRoute name='gift-cards' component={GiftCards}/>
      <Route name='gift-cards-new' path='new' component={NewGiftCard} />
      <Route name='giftcard' path=':giftCard' component={GiftCard}>
        <IndexRoute name='gift-card-transactions' component={GiftCardTransactions} />
        <Route name='gift-card-notes' path='notes' component={Notes} />
        <Route name='gift-card-activity-trail' path='activity-trail' component={ActivityTrailPage} />
      </Route>
    </Route>
    <Route name='style-guide' path='style-guide' component={StyleGuide}>
      <IndexRoute name='style-guide-grid' component={StyleGuideGrid} />
      <Route name='style-guide-buttons' path='buttons' component={StyleGuideButtons} />
      <Route name='style-guide-containers' path='containers' component={StyleGuideContainers} />
    </Route>
  </Route>
);

export default routes;
