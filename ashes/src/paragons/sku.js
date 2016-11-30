/* @flow */

import _, { cloneDeep } from 'lodash';
import { getJWT } from 'lib/claims';
import * as t from 'paragons/object-types';


import type { JWT } from 'lib/claims';
import type { Sku } from 'modules/skus/details';
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

// HACK
function isMerchant(): boolean {
  const jwt = getJWT();
  if (jwt != null && jwt.scope == '1') {
    return false;
  }

  return true;
}

export function createEmptySku(): Sku {
  let merchantAttributes = {};

  if (isMerchant()) {
    merchantAttributes = {
      externalId: t.string(''),
      mpn: t.string(''),
      gtin: t.string(''),
      weight: t.string(''),
      height: t.string(''),
      width: t.string(''),
      depth: t.string(''),
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
