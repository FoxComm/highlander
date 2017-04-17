// @flow

export default function(error: string): string {
  if (/zip must fully match/.test(error)) {
    return 'Zip code is invalid';
  }

  return error;
}
