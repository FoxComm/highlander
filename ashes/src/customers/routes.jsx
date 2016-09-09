/* @flow */

import React, { Component, Element } from 'react';
import { Route, IndexRoute } from 'react-router';

import Customers from 'components/customers/customers';
import CustomersListPage from 'components/customers/list-page';
import NewCustomer from 'components/customers/new-customer';
import Groups from 'components/customers-groups/groups';
import Group from 'components/customers-groups/group';
import NewDynamicGroup from 'components/customers-groups/dynamic/new-group';
import EditDynamicGroup from 'components/customers-groups/dynamic/edit-group';
import Customer from 'components/customers/customer';
import CustomerDetails from 'components/customers/details';
import Notes from 'components/notes/notes';
import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import CustomerTransactions from 'components/customers/transactions/transactions';
import CustomerCart from 'components/customers/transactions/cart';
import CustomerItems from 'components/customers/transactions/items';
import StoreCredits from 'components/customers/store-credits/store-credits';
import StoreCreditsTransactions from 'components/customers/store-credits/transactions';
import NewStoreCredit from 'components/customers/store-credits/new-store-credit';

const customerRoutes = () => {
  return (
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
        <Route title='Transactions'
               name='customer-transactions'
               path='transactions'
               component={CustomerTransactions}/>
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
  );
};

export default customerRoutes;
