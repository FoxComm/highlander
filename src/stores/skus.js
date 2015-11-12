import BaseStore from '../lib/base-store';

class SkuStore extends BaseStore {
  get baseUri() { return '/products'; }
}

export default new SkuStore();
