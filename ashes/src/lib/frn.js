/* @flow */
import _ from 'lodash';
import type { Claims } from 'lib/claims';

export const frn = {
  activity: {
    coupon: 'frn:activity:coupon',
    customer: 'frn:activity:customer',
    giftCard: 'frn:activity:giftCard',
    inventory: 'frn:activity:inventory',
    order: 'frn:activity:order',
    product: 'frn:activity:product',
    promotion: 'frn:activity:promotion',
    sku: 'frn:activity:sku',
    user: 'frn:activity:user',
  },
  mkt: {
    coupon: 'frn:mkt:coupon',
    giftCard: 'frn:mkt:giftCard',
    giftCardTransaction: 'frn:mkt:giftCardTransaction',
    promotion: 'frn:mkt:promotion',
  },
  mdl: {
    shipment: 'frn:mdl:shipment',
    summary: 'frn:mdl:summary',
    transaction: 'frn:mdl:transaction',
  },
  note: {
    customer: 'frn:note:customer',
    giftCard: 'frn:note:giftCard',
    product: 'frn:note:product',
    promotion: 'frn:note:promotion',
    sku: 'frn:order:sku',
    order: 'frn:note:order',
    coupon: 'frn:note:coupon',
  },
  oms: {
    cart: 'frn:oms:cart',
    order: 'frn:oms:order',
    fraud: 'frn:oms:fraud',
  },
  pim: {
    album: 'frn:pim:album',
    product: 'frn:pim:product',
    sku: 'frn:pim:sku',
  },
  settings: {
    user: 'frn:settings:usr',
    plugin: 'frn:settings:plugin',
    application: 'frn:settings:application',
    shippingMethod: 'frn:settings:shippingMethod',
  },
  user: {
    customer: 'frn:usr:customer',
    customerCart: 'frn:usr:customer-cart',
    customerGroup: 'frn:usr:customer-group',
    customerTransaction: 'frn:usr:customer-transaction',
  },
};

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
}

export function merchant(): Claims {
  return {
    ...readAction(frn.pim.product),
    ...readAction(frn.pim.sku),
    ...readAction(frn.pim.album),
    ...readAction(frn.mdl.summary),
    ...readAction(frn.oms.order),
  };
}
