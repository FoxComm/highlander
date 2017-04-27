// @flow

export default function(error: string): string {
  if (/The credit card was declined/.test(error)) {
    return `Your credit card has been declined.
              Please try another card or contact your bank for more information`;
  } else if (/exp_year/.test(error)) {
    return 'Your card\'s expiration year is invalid';
  }

  return error;
}
