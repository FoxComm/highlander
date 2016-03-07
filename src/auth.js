import _ from 'lodash';

function isServerSide() {
  return typeof window === "undefined";
}

export function isAuthenticated() {
  if (isServerSide()) return null;

  const token = localStorage.getItem('jwt');
  if (!token) return false;

  const user = localStorage.getItem("user");
  if (new Date() > new Date(user.exp*1000)) {
    return false;
  }

  return true;
}

export function currentUser() {
  if (isServerSide()) return null;

  const user = localStorage.getItem('user');
  if (user) {
    return JSON.parse(user);
  }
}

export const cachedCurrentUser = _.memoize(currentUser);
