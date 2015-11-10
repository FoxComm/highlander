import HashStore from '../lib/hash-store';

class AddressStore extends HashStore {
  baseUri(customerId) {
    return `/customers/${customerId}/addresses`;
  }
}

export default new AddressStore();
