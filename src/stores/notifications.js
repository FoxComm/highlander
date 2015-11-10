import BaseStore from '../lib/base-store';
import Api from '../lib/api';

class NotificationStore extends BaseStore {
  get baseUri() { return `${this.rootUri}/notifications`; }
  set uriRoot(uri) { this.rootUri = uri; }

  resend(id) {
    Api.post(this.uri(id))
      .then((res) => { this.update(res); })
      .catch((err) => { this.fetchError(err); });
  }
}

export default new NotificationStore();
