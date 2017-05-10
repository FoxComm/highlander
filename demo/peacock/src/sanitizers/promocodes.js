// @flow

export default function(error: string): string {
  if (error.startsWith('giftCard') && error.endsWith('not found')) {
    return 'Sorry, this gift card doesn\'t seem to be registered. Try another one.';
  } else if (error.startsWith('Gift Card') && /already added as payment method /.test(error)) {
    return 'This gift card was already added as a payment method.';
  } else if (error.startsWith('Gift Card') && error.endsWith('is inactive')) {
    return 'This gift card appears to be inactive. Try another one.';
  } else if (error.startsWith('couponCode') && error.endsWith('not found')) {
    return 'The coupon with this code doesn\'t exist. Try another one.';
  }

  return error;
}
