import React from 'react';
import { Route, IndexRoute, IndexRedirect } from 'react-router';
import Site from './components/site/site';
import Home from './components/home/home';
import Rmas from './components/rmas/rmas';
import Rma from './components/rmas/rma';
import RmaChildList from './components/rmas/child-list';
import RmaDetails from './components/rmas/details';
import OrdersListPage from './components/orders/list-page';
import Orders from './components/orders/orders';
import Order from './components/orders/order';
import OrderDetails from './components/orders/details';
import NewOrder from './components/orders/new-order';
import Customers from './components/customers/customers';
import CustomersListPage from './components/customers/list-page';
import NewCustomer from './components/customers/new-customer';
import Groups from './components/customers-groups/groups';
import Group from './components/customers-groups/group';
import NewDynamicGroup from './components/customers-groups/dynamic/new-group';
import EditDynamicGroup from './components/customers-groups/dynamic/edit-group';
import Customer from './components/customers/customer';
import CustomerDetails from './components/customers/details';
import Notes from './components/notes/notes';
import Notifications from './components/notifications/notifications';
import ActivityTrailPage from './components/activity-trail/activity-trail-page';
import GiftCards from './components/gift-cards/gift-cards';
import GiftCardsListPage from './components/gift-cards/list-page';
import NewGiftCard from './components/gift-cards/gift-cards-new';
import GiftCard from './components/gift-cards/gift-card';
import GiftCardTransactions from './components/gift-cards/transactions';
import StoreCredits from './components/customers/store-credits/store-credits';
import StoreCreditsTransactions from './components/customers/store-credits/transactions';
import NewStoreCredit from './components/customers/store-credits/new-store-credit';
import CustomerTransactions from './components/customers/transactions/transactions';
import CustomerCart from './components/customers/transactions/cart';
import CustomerItems from './components/customers/transactions/items';
import InventoryListPage from './components/inventory/list-page';
import InventoryList from './components/inventory/list';
import InventoryItem from './components/inventory/item';
import InventoryItemDetailsBase from './components/inventory/item-details-base';
import InventoryItemDetails from './components/inventory/item-details';
import InventoryItemTransactions from './components/inventory/item-transactions';
import ProductsListPage from './components/products/list-page';
import Products from './components/products/products';
import ProductDetails from './components/products/details';
import NewProduct from './components/products/new-product';

import StyleGuide from './components/style-guide/style-guide';
import StyleGuideGrid from './components/style-guide/style-guide-grid';
import StyleGuideButtons from './components/style-guide/style-guide-buttons';
import StyleGuideContainers from './components/style-guide/style-guide-containers';

import AllActivities from './components/activity-trail/all';
import AllNotificationItems from './components/activity-notifications/all';
import Login from './components/auth/login';

const routes = (
  <Route path="/">
    <Route name="login" path="login" component={Login}/>
    <Route component={Site}>
    <IndexRoute name="home" component={Home}/>
    <IndexRedirect from="/" to="/orders/" />
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
      <Route name='new-order' path="new" component={NewOrder}/>
      <Route name='orders-list-pages' component={OrdersListPage}>
        <IndexRoute name='orders' component={Orders}/>
        <Route name='orders-activity-trail' path='activity-trail' dimension="order"
               component={ActivityTrailPage}/>
      </Route>

      <Route name='order' path=':order' component={Order}>
        <IndexRoute name='order-details' component={OrderDetails}/>
        <Route name='order-notes' path='notes' component={Notes}/>
        <Route name='order-returns' path='returns' component={RmaChildList}/>
        <Route name='order-notifications' path='notifications' component={Notifications}/>
        <Route name='order-activity-trail' path='activity-trail' component={ActivityTrailPage}/>
      </Route>
    </Route>
    <Route name='customers-base' path='customers'>
      <Route name='customers-list-pages' component={CustomersListPage}>
        <IndexRoute name='customers' component={Customers}/>
        <Route name='customers-activity-trail' path='activity-trail' dimension="customer"
               component={ActivityTrailPage}/>
      </Route>
      <Route name='groups-base' path='groups'>
        <Route name='customer-groups' component={CustomersListPage}>
          <IndexRoute name='groups' component={Groups} />
        </Route>
        <Route name='new-dynamic-customer-group' path='new-dynamic' component={NewDynamicGroup} />
        <Route name='edit-dynamic-customer-group' path='edit-dynamic/:groupId' component={EditDynamicGroup} />
        <Route title='Group' name='customer-group' path=':groupId' component={Group} />
      </Route>
      <Route name='customers-new' path='new' component={NewCustomer} />
      <Route name='customer' path=':customerId' component={Customer}>
        <IndexRoute name='customer-details' component={CustomerDetails}/>
        <Route title='Transactions' name='customer-transactions' path='transactions' component={CustomerTransactions}/>
        <Route title='Returns' name='customer-returns' path='returns' component={RmaChildList}/>
        <Route title='Cart' name='customer-cart' path='cart' component={CustomerCart}/>
        <Route title='Items' name='customer-items' path='items' component={CustomerItems}/>
        <Route name='customer-notes' path='notes' component={Notes} />
        <Route name='customer-activity-trail' path='activity-trail' component={ActivityTrailPage}/>
        <Route name='customer-storecredits-base' path='storecredit'>
          <IndexRoute name='customer-storecredits' component={StoreCredits}/>
          <Route name='customer-storecredit-transactions'
                 path='transactions'
                 component={StoreCreditsTransactions} />
        </Route>
      </Route>
      <Route name='customer-storecredits-new'
             path=':customerId/storecredits/new'
             component={NewStoreCredit} />
    </Route>
    <Route name='products-base' path='products'>
      <Route name='products-list-pages' component={ProductsListPage}>
        <IndexRoute name='products' component={Products} />
      </Route>
      <Route name='new-product' path='new' component={NewProduct} />
      <Route name='product-details' path=':productId' component={ProductDetails} />
    </Route>
    <Route name='gift-cards-base' path='gift-cards'>
      <Route name='gift-cards-list-page' component={GiftCardsListPage}>
        <IndexRoute name='gift-cards' component={GiftCards}/>
        <Route name='gift-cards-activity-trail' path='activity-trail' dimension="gift-card"
               component={ActivityTrailPage}/>
      </Route>
      <Route name='gift-cards-new' path='new' component={NewGiftCard} />
      <Route name='giftcard' path=':giftCard' component={GiftCard}>
        <IndexRoute name='gift-card-transactions' component={GiftCardTransactions} />
        <Route name='gift-card-notes' path='notes' component={Notes} />
        <Route name='gift-card-activity-trail' path='activity-trail' component={ActivityTrailPage} />
      </Route>
    </Route>
    <Route name='inventory-base' path='inventory'>
      <Route name='inventory-list-page' component={InventoryListPage}>
        <IndexRoute name='inventory' component={InventoryList}/>
        <Route name='inventory-activity-trail' path='activity-trail' dimension="inventory" component={ActivityTrailPage}/>
      </Route>
      <Route name='inventory-item-base' path=':code' component={InventoryItem}>
        <Route name='inventory-item-details-base' component={InventoryItemDetailsBase}>
          <IndexRoute name='inventory-item-details' component={InventoryItemDetails} />
          <Route title='Transactions' name='inventory-item-transactions' path='transactions' component={InventoryItemTransactions} />
        </Route>
        <Route name='inventory-item-activity-trail' path='activity-trail' component={ActivityTrailPage} />
        <Route name='inventory-item-notes' path='notes' component={Notes} />
      </Route>
    </Route>
    <Route name='style-guide' path='style-guide' component={StyleGuide}>
      <IndexRoute name='style-guide-grid' component={StyleGuideGrid} />
      <Route name='style-guide-buttons' path='buttons' component={StyleGuideButtons} />
      <Route name='style-guide-containers' path='containers' component={StyleGuideContainers} />
    </Route>
    <Route name='test' path="_">
      <Route name='test-activities' path='activities' component={AllActivities} />
      <Route name='test-notifications' path='notifications' component={AllNotificationItems} />
    </Route>
  </Route>
  </Route>
);

export default routes;
