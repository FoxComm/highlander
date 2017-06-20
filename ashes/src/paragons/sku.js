/* @flow */

import _, { cloneDeep } from 'lodash';
import * as t from 'paragons/object-types';

export function generateSkuCode(): string {
  return Math.random().toString(36).substring(7).toUpperCase();
}

// should contain all known attributes
export const skuEmptyAttributes = {
  code: t.string(''),
  title: t.string(''),
  upc: t.string(''),
  description: t.richText(''),
  retailPrice: t.price({ currency: 'USD', value: 0 }),
  salePrice: t.price({ currency: 'USD', value: 0 }),
  unitCost: t.price({ currency: 'USD', value: 0 }),
};

export function createEmptySku(): Sku {
  return {
    id: null,
    attributes: { ...cloneDeep(skuEmptyAttributes) },
    context: {
      name: 'default',
    }
  };
}
