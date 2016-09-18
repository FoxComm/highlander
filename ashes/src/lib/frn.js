/* @flow */
import _ from 'lodash';
import type { Claims } from 'lib/claims';

export const frn = {
  mkt: {
    coupon: 'frn:mkt:coupon',
    giftCard: 'frn:mkt:giftCard',
    promotion: 'frn:mkt:promotion',
  },
  mdl: {
    summary: 'frn:mdl:summary',
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
    customerGroup: 'frn:usr:customer-group',
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
