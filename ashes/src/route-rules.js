/* @flow */

const authFreeUrls = /\/(login|signup|(reset|restore)-password)$/;

export function isPathRequiredAuth(path: string): boolean {
  return !path.match(authFreeUrls);
}
