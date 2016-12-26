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
  unitCost: t.price({ currency: 'USD', value: 0 }),
  taxClass: 'default',
  requiresShipping: true,
  shippingClass: 'default',
  isReturnable: true,
  returnWindow: {
    "value": '',
    "units": "days"
  },
  height: {
    "value": '',
    "units": "in"
  },
  weight: {
    "value": '',
    "units": "lbs"
  },
  length: {
    "value": '',
    "units": "in"
  },
  width: {
    "value": '',
    "units": "in"
  },
  requiresInventoryTracking: true,
  inventoryWarningLevel: {
    "isEnabled": true,
    "level": ''
  },
  maximumQuantityInCart: {
    "isEnabled": true,
    "level": ''
  },
  minimumQuantityInCart: {
    "isEnabled": false,
    "level": ''
  },
  allowBackorder: false,
  allowPreorder: false,
  requiresLotTracking: true,
  lotExpirationThreshold: {
    "value": '',
    "units": "months"
  },
  lotExpirationWarningThreshold: {
    "value": '',
    "units": "days"
  }
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
      depth: t.string(''),
    };
  }

  return {
    id: null,
    attributes: { ...cloneDeep(skuEmptyAttributes), ...merchantAttributes },
  };
}
