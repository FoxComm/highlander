/* @flow */
import _ from 'lodash';
import type { Claims } from 'lib/claims';

export const frn = {
  activity: {
    cart: 'frn:activity:cart',
    coupon: 'frn:activity:coupon',
    customer: 'frn:activity:customer',
    giftCard: 'frn:activity:giftCard',
    contentType: 'frn:activity:contentType',
    inventory: 'frn:activity:inventory',
    order: 'frn:activity:order',
    product: 'frn:activity:product',
    promotion: 'frn:activity:promotion',
    sku: 'frn:activity:sku',
    user: 'frn:activity:user',
  },
  merch: {
    taxonomy: 'frn:merch:taxonomy',
    taxon: 'frn:merch:taxon',
  },
  mkt: {
    coupon: 'frn:mkt:coupon',
    giftCard: 'frn:mkt:giftCard',
    giftCardTransaction: 'frn:mkt:giftCardTransaction',
    contentType: 'frn:mkt:contentType',
    contentTypeTransaction: 'frn:mkt:contentTypeTransaction',
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
    contentType: 'frn:note:contentType',
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
      // Dirty hack for feature switches. Just remove the permission to the module.
      switch (resourceClaim) {
        // case frn.merch.taxonomy:
        //   return systemList;
        default:
          return { ...systemList, [resourceClaim]: ['c', 'r', 'u', 'd'] };
      }
    }, {});

    return { ...claimsList, ...leaves };
  }, {});
}

export function merchant(): Claims {
  return _.reduce(frn, (claimsList, systemClaims) => {
    const leaves = _.reduce(systemClaims, (systemList, resourceClaim) => {
      // Dirty hack for feature switches. Just remove the permission to the module.
      switch (resourceClaim) {
        case frn.settings.plugin:
          return systemList;
        case frn.settings.application:
          return systemList;
        default:
          return { ...systemList, [resourceClaim]: ['c', 'r', 'u', 'd'] };
      }
    }, {});

    return { ...claimsList, ...leaves };
  }, {});
}
