/* @flow */

type MaybeString = string|void;

export function detectCardType(cardNumber: MaybeString): MaybeString {
  if (cardNumber == void 0) return;

  if (/^3[47]/.test(cardNumber)) {
    return 'amex';
  } else if (/^30[0-5]/.test(cardNumber) || /^3[68]/.test(cardNumber)) {
    return 'dinners-club';
  } else if (/^5[1-5]/.test(cardNumber)) {
    return 'master-card';
  } else if (/^4/.test(cardNumber)) {
    return 'visa';
  }
}

export function cardMask(cardType: MaybeString): string {
  switch (cardType) {
    case 'amex':
      return '9999 999999 99999';
    case 'dinners-club':
      return '9999 999999 9999';
    default:
      return '9999 9999 9999 9999';
  }
}

export function cvvLength(cardType: MaybeString): number {
  if (cardType == 'amex') {
    return 4;
  }
  return 3;
}

export function trimWhiteSpace(cardNumber: string): string {
  return cardNumber.replace(/\s/g, '');
}

export function isCardNumberValid(cardNumber: string): boolean {
  const mask = cardMask(detectCardType(cardNumber)).replace(/[^\d]/g, '');

  return mask.length === trimWhiteSpace(cardNumber).length;
}

export function isCvvValid(cvv: string, cardType: string): boolean {
  return cvv.length == cvvLength(cardType)
}
