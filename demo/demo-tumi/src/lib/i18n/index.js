export default from './localized';

export type Localized = {
  t: (message: string, plural: ?string, count: ?number) => string;
};

export function phoneMask() {
  return '(999) 999-9999';
}
