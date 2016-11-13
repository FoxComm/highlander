
const authFreeUrls = /\/(login|signup)$/;

export function isPathRequiredAuth(path) {
  return !path.match(authFreeUrls);
}
