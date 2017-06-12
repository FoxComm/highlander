import $ from '../payloads';

export default async (api, customerId) => {
  const creditCardDetails = $.randomCreditCardDetailsPayload(customerId);
  const newTokenResponse = await api.dev.creditCardToken(creditCardDetails);
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
  return api.customerCreditCards.add(customerId, payload);
};
