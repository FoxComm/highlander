/* @flow */

import { assoc } from 'sprout-data';
import { cloneDeep } from 'lodash';

import type { Sku } from '../modules/skus/details';

export const options = {
  code: { label: 'SKU' },
  upc: { label: 'UPC' },
};

export function generateSkuCode(): string {
  return Math.random().toString(36).substring(7).toUpperCase();
}

// should contain all known attributes
export const skuEmptyAttributes = {
  code: { t: 'string', v: '' },
  title: { t: 'string', v: '' },
  upc: { t: 'string', v: '' },
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
};

export function createEmptySku(): Sku {
  return {
    id: null,
    attributes: cloneDeep(skuEmptyAttributes),
    context: {
      name: 'default',
    }
  };
}

export function updateFieldLabels(sku: Sku): Sku {
  return assoc(sku,
    ['attributes', 'code', 'label'], 'SKU',
    ['attributes', 'upc', 'label'], 'UPC',
  );
}
