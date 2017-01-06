/* @flow */

import _, { cloneDeep } from 'lodash';
import { getJWT } from 'lib/claims';
import * as t from 'paragons/object-types';

import type { JWT } from 'lib/claims';
import type { ProductVariant } from 'modules/product-variants/details';
export function generateSkuCode(): string {
  return Math.random().toString(36).substring(7).toUpperCase();
}

// should contain all known attributes
export const productVariantEmptyAttributes = {
  code: t.string(''),
  title: t.string(''),
  upc: t.string(''),
  unitCost: t.price({ currency: 'USD', value: 0 }),
  taxClass: t.string('default'),
  requiresShipping: t.bool(true),
  shippingClass: t.string('default'),
  isReturnable: t.bool(true),
  returnWindow: {
    'value': 30,
    'units': 'days'
  },
  height: {
    'value': 0,
    'units': 'in'
  },
  weight: {
    'value': 0,
    'units': 'lbs'
  },
  length: {
    'value': 0,
    'units': 'in'
  },
  width: {
    'value': 0,
    'units': 'in'
  },
  requiresInventoryTracking: t.bool(true),
  inventoryWarningLevel: {
    'isEnabled': false,
    'level': 0
  },
  maximumQuantityInCart: {
    'isEnabled': false,
    'level': 10
  },
  minimumQuantityInCart: {
    'isEnabled': false,
    'level': 1
  },
  allowBackorder: t.bool(false),
  allowPreorder: t.bool(false),
  requiresLotTracking: t.bool(true),
  lotExpirationThreshold: {
    'value': 0,
    'units': 'months'
  },
  lotExpirationWarningThreshold: {
    'value': 0,
    'units': 'days'
  }
};

export function configureProductVariant(productVariant: ProductVariant): ProductVariant {
  _.defaults(productVariant.attributes, productVariantEmptyAttributes);

  return productVariant;
}

// HACK
function isMerchant(): boolean {
  const jwt = getJWT();
  if (jwt != null && jwt.scope == '1') {
    return false;
  }

  return true;
}

export function createEmptyProductVariant(): ProductVariant {
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
    attributes: { ...cloneDeep(productVariantEmptyAttributes), ...merchantAttributes },
    context: {
      name: 'default',
    }
  };
}
