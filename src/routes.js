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
import Customer from './components/customers/customer';
import CustomerDetails from './components/customers/details';
import Notes from './components/notes/notes';
import Notifications from './components/notifications/notifications';
import ActivityTrail from './components/activity-trail/activity-trail';
import GiftCards from './components/gift-cards/gift-cards';
import NewGiftCard from './components/gift-cards/gift-cards-new';
import GiftCard from './components/gift-cards/gift-card';
import GiftCardTransactions from './components/gift-cards/transactions';

import StyleGuide from './components/style-guide/style-guide';
import StyleGuideGrid from './components/style-guide/style-guide-grid';
import StyleGuideButtons from './components/style-guide/style-guide-buttons';
import StyleGuideContainers from './components/style-guide/style-guide-containers';

const routes = (
  <Route path="/" component={Site}>
    <IndexRoute name="home" component={Home}/>
    <Route name='orders' path="orders" component={Orders}/>
    <Route name='rmas' path='returns' component={Rmas}/>
    <Route name='rma' path='returns/:rma' component={Rma}>
      <IndexRoute name='rma-details' component={RmaDetails}/>
      <Route name='rma-notes' path='notes' component={Notes}/>
      <Route name='rma-notifications' path='notifications' component={Notifications}/>
      <Route name='rma-activity-trail' path='activity-trail' component={ActivityTrail}/>
    </Route>
    <Route name='order' path='orders/:order' component={Order}>
      <IndexRoute name='order-details' component={OrderDetails}/>
      <Route name='order-notes' path='notes' component={Notes}/>
      <Route name='order-returns' path='returns' component={RmaChildList}/>
      <Route name='order-notifications' path='notifications' component={Notifications}/>
      <Route name='order-activity-trail' path='activity-trail' component={ActivityTrail}/>
    </Route>
    <Route name='customers' path='customers' component={Customers}/>
    <Route name='customers-new' path='customers/new' component={NewCustomer} />
    <Route name='customer' path='customers/:customerId' component={Customer}>
      <IndexRoute name='customer-details' component={CustomerDetails}/>
      <Route name='customer-notes' path='notes' component={Notes} />
    </Route>
    <Route name='gift-cards' path='gift-cards' component={GiftCards} />
    <Route name='gift-cards-new' path='gift-cards/new' component={NewGiftCard} />
    <Route name='giftcard' path='gift-cards/:giftcard' component={GiftCard}>
      <IndexRoute name='gift-card-transactions' component={GiftCardTransactions} />
      <Route name='gift-card-notes' path='notes' component={Notes} />
      <Route name='gift-card-activity-trail' path='activity-trail' component={ActivityTrail} />
    </Route>
    <Route name='style-guide' path='style-guide' component={StyleGuide}>
      <IndexRoute name='style-guide-grid' component={StyleGuideGrid} />
      <Route name='style-guide-buttons' path='buttons' component={StyleGuideButtons} />
      <Route name='style-guide-containers' path='containers' component={StyleGuideContainers} />
    </Route>
  </Route>
);

export default routes;
