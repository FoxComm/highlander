// @flow
export const env = typeof window == 'undefined' ? process.env : window.env;

export function assetsUrl(path: string): string {
  return `${env.URL_PREFIX}${path}`;
}
