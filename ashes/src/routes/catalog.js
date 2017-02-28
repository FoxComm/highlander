/* @flow */
import React, { Component, Element } from 'react';

import FoxRouter from 'lib/fox-router';
import { frn } from 'lib/frn';

import Analytics from 'components/analytics/analytics';
import ActivityTrailPage from 'components/activity-trail/activity-trail-page';
import Notes from 'components/notes/notes';

import SkuListPage from '../components/skus/list-page';
import SkuList from '../components/skus/list';
import SkuPage from '../components/skus/page';
import SkuDetails from '../components/skus/details';

import ProductsListPage from 'components/products/list-page';
import Products from 'components/products/list/products';
import ProductPage from 'components/products/page';
import ProductForm from 'components/products/product-form';
import ProductImages from 'components/products/images';

import ProductVariantPage from 'components/product-variants/page';
import ProductVariantDetails from 'components/product-variants/details';
import ProductVariantImages from 'components/product-variants/images';
import ProductVariantInventory from 'components/product-variants/inventory';

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
        router.read('product-analytics', {
          title: 'Analytics',
          path: 'analytics',
          component: Analytics,
          frn: frn.activity.product,
        }),
      ]),
    ]);

  const productVariantRoutes =
    router.read('product-variants-base', { title: 'Product Variants', path: 'variants', frn: frn.pim.sku }, [
      router.read('product-variant', { path: ':productVariantId', component: ProductVariantPage }, [
        router.read('product-variant-details', { component: ProductVariantDetails, isIndex: true }),
        router.read('product-variant-images', {
          path: 'images',
          title: 'Images',
          component: ProductVariantImages,
          frn: frn.pim.album,
        }),
        router.read('product-variant-inventory', {
          path: 'inventory',
          component: ProductVariantInventory,
          frn: frn.mdl.summary,
        }),
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
      router.read('skus-base', { path: 'skus', frn: frn.mdl.summary }, [
        router.read('sku-list-page',{ component: SkuListPage }, [
          router.read('skus', { component: SkuList, isIndex: true }),
          router.read('skus-activity-trail', {
             path: 'activity-trail',
             dimension: 'inventory',
             component: ActivityTrailPage,
             frn: frn.activity.inventory,
          }),
        ]),
        router.read('sku', { path: ':skuId', component: SkuPage }, [
          router.read('sku-details', {
            component: SkuDetails,
            isIndex: true,
            layout: 'details',
          }),
          router.read('sku-inventory', {
            path: 'inventory',
            component: SkuDetails,
            layout: 'inventory',
            frn: frn.mdl.summary,
          }),
        ])
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
