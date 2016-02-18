import _ from 'lodash';

function isServerSide() {
  return typeof window === "undefined";
}

export function isAuthenticated() {
  if (isServerSide()) return null;

  return !!localStorage.getItem('jwt');
}

export function currentUser() {
  if (isServerSide()) return null;

  const token = localStorage.getItem('user');
  if (token) {
    return JSON.parse(token);
  }
}

export const cachedCurrentUser = _.memoize(currentUser);
