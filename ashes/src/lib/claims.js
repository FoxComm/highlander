/* @flow */

type Claims = { [claim:string]: Array<string> };

export function getClaims(): Claims {
  if (typeof(Storage) !== "undefined") {
    const token = localStorage.getItem("jwt");

    if (token) {
      // TODO: Add actual parsing of the JWT later.
      const claims = {
        'frn:oms:cart': ['c', 'r', 'u', 'd'],
        'frn:oms:order': ['c', 'r', 'u', 'd'],
        'frn:oms:my:cart': ['c', 'r', 'u', 'd'],
        'frn:mdl:summary': ['c', 'r', 'u', 'd'],
        'frn:pim:product': ['c', 'r', 'u', 'd'],
        'frn:pim:sku': ['c', 'r', 'u', 'd'],
        'frn:pim:album': ['c', 'r', 'u', 'd'],
        'frn:pim:coupon': ['c', 'r', 'u', 'd'],
        'frn:usr:user': ['c', 'r', 'u', 'd'],
        'frn:usr:role': ['c', 'r', 'u', 'd'],
        'frn:usr:org': ['c', 'r', 'u', 'd'],
        'frn:usr:my:info': ['c', 'r', 'u', 'd'],
      };

      return claims;
    }
  }

  return {};
}
