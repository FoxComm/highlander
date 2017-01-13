/* @flow */

import React, { Component, Element } from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import Customers from 'components/customers/customers';
import CustomersListPage from 'components/customers/list-page';
import NewCustomer from 'components/customers/new-customer';
import GroupsListPage from 'components/customers-groups/list-page';
import GroupsList from 'components/customers-groups/groups-list';
import EditGroupPage from 'components/customers-groups/edit-group-page';
import GroupPage from 'components/customers-groups/group-page';
import NewGroup from 'components/customers-groups/new-group';
import EditGroup from 'components/customers-groups/edit-group';
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

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const customerRoutes =
    router.read('customers-base', { path: 'customers', frn: frn.user.customer }, [
      router.read('customers-list-pages', { component: CustomersListPage }, [
        router.read('customers', { component: Customers, isIndex: true }),
        router.read('customers-activity-trail', {
          path: 'activity-trail',
          dimension: 'account',
          component: ActivityTrailPage,
          frn: frn.activity.customer,
         }),
      ]),
      router.read('groups-base', { path: 'groups', frn: frn.user.customerGroup }, [
        router.read('customer-groups', { component: GroupsListPage }, [
          router.read('groups', { component: GroupsList, isIndex: true }),
        ]),

        router.create('new-customer-group', { component: EditGroupPage }, [
          router.create('new-customer-group', { path: 'new', component: NewGroup }),
        ]),

        router.update('edit-customer-group', { component: EditGroupPage }, [
          router.update('edit-customer-group', { path: ':groupId/edit', component: EditGroup }),
        ]),

        router.read('customer-group', { path: ':groupId', title: 'Group', component: GroupPage }),
      ]),
      router.create('customers-new', { path: 'new', component: NewCustomer }),
      router.read('customer', { path: ':customerId', component: Customer }, [
        router.read('customer-details', { component: CustomerDetails, isIndex: true }),
        router.read('customer-transactions', {
          title: 'Transactions',
          path: 'transactions',
          component: CustomerTransactions,
          frn: frn.user.customerTransaction,
         }),
        router.read('customer-cart', {
          title: 'Cart',
          path: 'cart',
          component: CustomerCart,
          frn: frn.user.customerCart,
         }),
        router.read('customer-items', { title: 'Items', path: 'items', component: CustomerItems }),
        router.read('customer-notes', { path: 'notes', component: Notes }),
        router.read('customer-activity-trail', {
          path: 'activity-trail',
          dimension: 'account',
          component: ActivityTrailPage
        }),
        router.read('customer-storecredits-base', { path: 'storecredit' }, [
          router.read('customer-storecredits', { component: StoreCredits, isIndex: true }),
          router.read('customer-storecredit-transactions', { path: 'transactions',
              component: StoreCreditsTransactions }),
        ]),
      ]),
      router.read('customer-storecredits-new', { path: ':customerId/storecredits/new', component: NewStoreCredit }),
    ]);

  return customerRoutes;
};

export default getRoutes;
