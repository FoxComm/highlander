
/* @flow */

import _ from 'lodash';

export const authBlockTypes = {
  LOGIN: 'LOGIN',
  RESET_PASSWORD: 'RESET_PASSWORD',
  RESTORE_PASSWORD: 'RESTORE_PASSWORD',
  SIGNUP: 'SIGNUP',
};

export function isGuest(user: Object): boolean {
  const email = _.get(user, 'email', '');
  const name = _.get(user, 'name', '');
  return _.isEmpty(email) || _.isEmpty(name);
}

export function emailIsSet(user: Object): boolean {
  const email = _.get(user, 'email', '');
  return _.isEmpty(email);
}
