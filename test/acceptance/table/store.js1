'use strict';

import BaseStore from '../../../src/themes/admin/lib/base-store';

class TableStore extends BaseStore {
  fetch() {
    this.update([
      {id: 1, text: 'foo'},
      {id: 10, text: 'buzz'},
      {id: 2, text: 'bar'}
    ]);
  }

  patch(id, changes) {
  }

  create(data) {
  }

  getColumns() {
    return [
      {field: 'id', text: 'id', type: 'text'},
      {field: 'text', text: 'text', type: 'text'}
    ];
  }
}

export default new TableStore();
