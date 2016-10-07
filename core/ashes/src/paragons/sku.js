/* @flow */

import { assoc } from 'sprout-data';
import _, { cloneDeep } from 'lodash';
import { isSatisfied } from 'paragons/object';
import { getJWT } from 'lib/claims';

import type { Sku } from '../modules/skus/details';

export const options = {
  code: { label: 'SKU', required: true },
  title: { required: true },
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

export function isSkuValid(sku: Sku): boolean {
  return isSatisfied(sku, options);
}

// HACK
function isMerchant(): boolean {
  const jwt = getJWT();
  if (jwt.email == 'admin@admin.com') {
    return false;
  }

  return true;
}

export function createEmptySku(): Sku {
  let merchantAttributes = {};

  if (isMerchant()) {
    merchantAttributes = {
      externalId: {t: 'string', v: ''},
      mpn: { t: 'string', v: '' },
      gtin: { t: 'string', v: '' },
      weight: { t: 'string', v: '' },
      height: { t: 'string', v: '' },
      width: { t: 'string', v: '' },
      depth: { t: 'string', v: '' },
    };
  }

  return {
    id: null,
    attributes: { ...cloneDeep(skuEmptyAttributes), ...merchantAttributes },
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
