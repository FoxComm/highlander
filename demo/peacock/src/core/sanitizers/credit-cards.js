// @flow

export default function(error: string): string {
  if (/The credit card was declined/.test(error)) {
    return `Your Credit Card has been declined.
              Please try another card or contact your provider for more information`;
  }

  return error;
}
