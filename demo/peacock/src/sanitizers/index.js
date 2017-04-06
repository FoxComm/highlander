// @flow
import _ from 'lodash';
import sanitizeAddresses from './addresses';
import sanitizeCreditCards from './credit-cards';
import sanitizePromoCodes from './promocodes';

const sanitizers = [sanitizeAddresses, sanitizeCreditCards, sanitizePromoCodes];

export default function sanitizeAll(error: string): string {
  let result = error;

  _.some(sanitizers, (sanitizeError) => {
    const sanitized = sanitizeError(error);
    if (sanitized !== error) {
      result = sanitized;
      return true;
    }
  });

  return result;
}
