/* @flow */
import React, { Component, Element } from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import Notes from 'components/notes/notes';

import InventoryListPage from 'components/inventory/list-page';
import InventoryList from 'components/inventory/list';

import ProductsListPage from 'components/products/list-page';
import Products from 'components/products/products';
import ProductPage from 'components/products/page';
import ProductForm from 'components/products/product-form';
import ProductImages from 'components/products/images';

import ProductVariants from 'components/product-variants/skus';
import ProductVariantsListPage from 'components/product-variants/list-page';
import ProductVariantPage from 'components/product-variants/page';
import ProductVariantDetails from 'components/product-variants/details';
import ProductVariantImages from 'components/product-variants/images';
import PVInventoryPage from 'components/product-variants/inventory/page';
import PVInventoryDetails from 'components/product-variants/inventory/details';

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
      ]),
    ]);

  const productVariantRoutes =
    router.read('product-variants-base', { title: 'Product Variants', path: 'variants', frn: frn.pim.sku }, [
      router.read('product-variants-list-pages', { component: ProductVariantsListPage }, [
        router.read('variants', { component: ProductVariants, isIndex: true }),
        router.read('product-variants-activity-trail', {
          path: 'activity-trail',
          dimension: 'product-variant',
          component: ActivityTrailPage,
          frn: frn.activity.sku,
        }),
      ]),
      router.read('product-variant', { path: ':productVariantId', component: ProductVariantPage }, [
        router.read('product-variant-details', { component: ProductVariantDetails, isIndex: true }),
        router.read('product-variant-images', {
          path: 'images',
          title: 'Images',
          component: ProductVariantImages,
          frn: frn.pim.album,
        }),
        router.read('product-variant-inventory-details-base', {
          path: 'inventory',
          component: PVInventoryPage,
          frn: frn.mdl.summary,
        }, [
          router.read('product-variant-inventory-details', {
            component: PVInventoryDetails,
            isIndex: true,
          })
        ]),
        router.read('product-variant-notes', {
          path: 'notes',
          title: 'Notes',
          component: Notes,
          frn: frn.note.sku,
        }),
        router.read('product-variant-activity-trail', {
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
      {productVariantRoutes}
      {inventoryRoutes}
    </div>
  );
};

export default getRoutes;
