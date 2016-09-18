/* @flow */
import _ from 'lodash';
import type { Claims } from 'lib/claims';

export const frn = {
  activity: {
    customer: 'frn:activity:customer',
    inventory: 'frn:activity:inventory',
    order: 'frn:activity:order',
    product: 'frn:activity:product',
    sku: 'frn:activity:sku',
    user: 'frn:activity:user',
  },
  mkt: {
    coupon: 'frn:mkt:coupon',
    giftCard: 'frn:mkt:giftCard',
    promotion: 'frn:mkt:promotion',
  },
  mdl: {
    shipment: 'frn:mdl:shipment',
    summary: 'frn:mdl:summary',
    transaction: 'frn:mdl:transaction',
  },
  note: {
    customer: 'frn:note:customer',
    product: 'frn:note:product',
    sku: 'frn:order:sku',
    order: 'frn:note:order',
  },
  oms: {
    cart: 'frn:oms:cart',
    order: 'frn:oms:order',
  },
  pim: {
    album: 'frn:pim:album',
    product: 'frn:pim:product',
    sku: 'frn:pim:sku',
  },
  settings: {
    user: 'frn:settings:usr',
    plugin: 'frn:settings:plugin',
  },
  user: {
    customer: 'frn:usr:customer',
    customerCart: 'frn:usr:customer-cart',
    customerGroup: 'frn:usr:customer-group',
    customerTransaction: 'frn:usr:customer-transaction',
  },
}

export function readAction(frn: string): Claims {
  return { [frn]: ['r'] };
}

export function superAdmin(): Claims {
  return _.reduce(frn, (claimsList, systemClaims) => {
    const leaves = _.reduce(systemClaims, (systemList, resourceClaim) => {
      return { ...systemList, [resourceClaim]: ['c', 'r', 'u', 'd'] };
    }, {});

    return { ...claimsList, ...leaves };
  }, {});
};

export function merchant(): Claims {
  return {
    ...readAction(frn.pim.product),
    ...readAction(frn.pim.sku),
    ...readAction(frn.pim.album),
    ...readAction(frn.mdl.summary),
    ...readAction(frn.oms.order),
  };
}
