/* @flow */

import { assoc } from 'sprout-data';

import type { Sku } from '../modules/skus/details';

export function generateSkuCode(): string {
  return Math.random().toString(36).substring(7).toUpperCase();
}

export function createEmptySku(): Sku {
  const emptySku = {
    id: null,
    attributes: {
      code: { label: 'SKU', t: 'string', v: '' },
      title: { t: 'string', v: '' },
      upc: { label: 'UPC', t: 'string', v: '' },
      description: { t: 'richText', v: '' },
      retailPrice: {
        t: 'price',
        v: { currency: 'USD', value: 0 },
      },
      salePrice: {
        t: 'price',
        v: { currency: 'USD', value: 0 },
      },
      unitCost: {
        t: 'price',
        v: { currency: 'USD', value: 0 },
      },
    },
  };

  return emptySku;
}

export function updateFieldLabels(sku: Sku) {
  return assoc(sku,
    ['attributes', 'code', 'label'], 'SKU',
    ['attributes', 'upc', 'label'], 'UPC',
  );
}
