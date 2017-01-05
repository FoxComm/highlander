/* @flow */
import React, { Component, Element } from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import Insights from 'components/insights/insights';
import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import Notes from 'components/notes/notes';

import InventoryListPage from 'components/inventory/list-page';
import InventoryList from 'components/inventory/list';
import InventoryItemDetailsBase from 'components/inventory/item-details-base';
import InventoryItemDetails from 'components/inventory/item-details';
import InventoryItemTransactions from 'components/inventory/item-transactions';

import ProductsListPage from 'components/products/list-page';
import Products from 'components/products/products';
import ProductPage from 'components/products/page';
import ProductForm from 'components/products/product-form';
import ProductImages from 'components/products/images';

import Skus from 'components/skus/skus';
import SkusListPage from 'components/skus/list-page';
import SkuPage from 'components/skus/page';
import SkuDetails from 'components/skus/details';
import SkuImages from 'components/skus/images';

import type { Claims } from 'lib/claims';

const getRoutes = (jwt: Object) => {
  const router = new FoxRouter(jwt);

  const productRoutes =
    router.read('products-base', { path: 'products', frn: frn.pim.product }, [
      router.read('products-list-pages', { component: ProductsListPage }, [
        router.read('products', { component: Products, isIndex: true }),
        router.read('products-activity-trail', {
          path: 'activity-trail',
          dimension: 'product',
          component: ActivityTrailPage,
          frn: frn.activity.product,
        }),
      ]),
      router.read('product', {
        path: ':context/:productId',
        titleParam: ':productId',
        component: ProductPage,
      }, [
        router.read('product-details', { component: ProductForm, isIndex: true }),
        router.create('new-product', { component: ProductForm }),
        router.read('product-images', {
          title: 'Images',
          path: 'images',
          component: ProductImages,
          frn: frn.pim.album,
        }),
        router.read('product-notes', {
          title: 'Notes',
          path: 'notes',
          component: Notes,
          frn: frn.note.product,
        }),
        router.read('product-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.product,
        }),
        router.read('product-insights', {
          title: 'Insights',
          path: 'insights',
          component: Insights,
          frn: frn.activity.product,
        }),
      ]),
    ]);

  const skuRoutes =
    router.read('skus-base', { title: 'SKUs', path: 'skus', frn: frn.pim.sku }, [
      router.read('skus-list-pages', { component: SkusListPage }, [
        router.read('skus', { component: Skus, isIndex: true }),
        router.read('skus-activity-trail', {
          path: 'activity-trail',
          dimension: 'sku',
          component: ActivityTrailPage,
          frn: frn.activity.sku,
        }),
      ]),
      router.read('sku', { path: ':skuCode', component: SkuPage }, [
        router.read('sku-details', { component: SkuDetails, isIndex: true }),
        router.read('sku-images', {
          path: 'images',
          title: 'Images',
          component: SkuImages,
          frn: frn.pim.album,
        }),
        router.read('sku-inventory-details-base', {
          path: 'inventory',
          component: InventoryItemDetailsBase,
          frn: frn.mdl.summary,
        }, [
          router.read('sku-inventory-details', {
            component: InventoryItemDetails,
            isIndex: true,
          }),
          router.read('sku-inventory-transactions', {
            title: 'Transactions',
            path: 'transactions',
            component: InventoryItemTransactions,
            frn: frn.mdl.transaction,
          }),
        ]),
        router.read('sku-notes', {
          path: 'notes',
          title: 'Notes',
          component: Notes,
          frn: frn.note.sku,
        }),
        router.read('sku-activity-trail', {
          path: 'activity-trail',
          component: ActivityTrailPage,
          frn: frn.activity.sku,
        }),
      ]),
    ]);

  const inventoryRoutes =
      router.read('inventory-base', { path: 'inventory', frn: frn.mdl.summary }, [
        router.read('inventory-list-page',{ component: InventoryListPage }, [
          router.read('inventory', { component: InventoryList, isIndex: true }),
          router.read('inventory-activity-trail', {
             path: 'activity-trail',
             dimension: 'inventory',
             component: ActivityTrailPage,
             frn: frn.activity.inventory,
          }),
        ]),
      ]);

  return (
    <div>
      {productRoutes}
      {skuRoutes}
      {inventoryRoutes}
    </div>
  );
};

export default getRoutes;
