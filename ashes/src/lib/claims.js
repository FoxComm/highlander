/* @flow */
import _ from 'lodash';
import jwt from 'jsonwebtoken';

export type Claims = { [claim:string]: Array<string> };

export type JWT = {
  admin: boolean,
  aud: string,
  email: string,
  exp: number,
  id: number,
  iss: string,
  name: string,
  ratchet: number,
  claims: Claims,
};

export function getJWT(): ?JWT {
  // Make sure that this only runs in the browser.
  if (typeof(Storage) === "undefined") {
    return null;
  }

  // Get the JWT.
  const token = localStorage.getItem("jwt");
  if (!token) {
    return null;
  }

  // Decrypt the JWT
  return jwt.decode(token);
}

export function getClaims(): Claims {
  const token = getJWT();
  if (!token) {
    return {};
  }

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

    'frn:usr:customer': ['r'],
    'frn:usr:customer-groups': ['r'],
  };

  return claims;
}

export function isPermitted(expectedClaims: Claims, actualClaims: Claims): boolean {
  return _.reduce(expectedClaims, (res, expActions, expFRN) => {
    const actions = _.get(actualClaims, expFRN);
    if (!actions) {
      return false;
    }

    const hasAction = _.reduce(expActions, (actionRes, expAction) => {
      return actionRes && _.includes(actions, expAction);
    }, true);

    return res && hasAction;
  }, true);
}

export function anyPermitted(expectedClaims: Claims, actualClaims: Claims): boolean {
  const isRestricted = _.reduce(expectedClaims, (res, expActions, expFRN) => {
    const actions = _.get(actualClaims, expFRN);
    if (!actions) {
      return true;
    }

    const hasAction = _.reduce(expActions, (actionRes, expAction) => {
      return actionRes && _.includes(actions, expAction);
    }, true);

    return res && !hasAction;
  }, true);

  return !isRestricted;
}
