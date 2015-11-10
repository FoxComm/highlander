import BaseStore from '../lib/base-store';

class ViewerStore extends BaseStore {
  get baseUri() { return `${this.rootUri}/viewers`; }
  set uriRoot(uri) { this.rootUri = uri; }
}

export default new ViewerStore();
