/* @flow */

export function ascii(value: string, label: string): ?string {
  return /^[\x00-\x7F]+$/.test(value) ? null : `${label} must contain only ASCII characters`;
}

export function phoneNumber(value: string, label: string): ?string {
  return /^[\d#\-\(\)\+\*) ]+$/.test(value) ? null : `${label} must not contain letters or other non-valid characters`;
}

export function email(value: string): boolean {
  return /.+@.+/.test(value);
}
