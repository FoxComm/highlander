'use strict';

import BaseStore from '../lib/base-store';

class NoteStore extends BaseStore {
  get baseUri() { return `${this.rootUri}/notes`; }
  set uriRoot(uri) { this.rootUri = uri; }
}

export default new NoteStore();
