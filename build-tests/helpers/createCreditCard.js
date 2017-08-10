import $ from '../payloads';
import * as step from './steps';

export default async (api, customerId) => {
  const creditCardDetails = $.randomCreditCardDetailsPayload(customerId);
  const newTokenResponse = await step.getCreditCardToken(api, creditCardDetails);
  const payload = {
    token: newTokenResponse.token,
    lastFour: newTokenResponse.lastFour,
    expYear: creditCardDetails.expYear,
    expMonth: creditCardDetails.expMonth,
    brand: newTokenResponse.brand,
    holderName: creditCardDetails.address.name,
    billingAddress: creditCardDetails.address,
    addressIsNew: true,
  };
  return step.addCustomerCreditCard(api, customerId, payload);
};
