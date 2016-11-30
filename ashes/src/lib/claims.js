/* @flow */
import _ from 'lodash';
import { merchant, superAdmin } from 'lib/frn';
import jwtDecode from 'jwt-decode';

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
  if (typeof(Storage) === 'undefined') {
    return null;
  }

  // Get the JWT.
  const token = localStorage.getItem('jwt');
  if (!token) {
    return null;
  }

  // Decrypt the JWT
  return jwtDecode(token);
}

export function getClaims(token: ?JWT = getJWT()): Claims {
  if (!token) {
    return {};
  }

  return token.scope == '1'
    ? superAdmin()
    : merchant();
}

export function getUserId(): ?number {
  const token = getJWT();
  if (!token) {
    return null;
  }

  return token.id;
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
      return res && true;
    }

    const hasAction = _.reduce(expActions, (actionRes, expAction) => {
      return actionRes && _.includes(actions, expAction);
    }, true);

    return res && !hasAction;
  }, true);

  return !isRestricted;
}
