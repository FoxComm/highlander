'use strict';

import TableStore from '../../lib/table-store';

class NoteStore extends TableStore {
  constructor(...args) {
    super(...args);
    this.columns = [
      {
        title: 'Date/Time',
        field: 'createdAt'
      },
      {
        title: 'Text',
        field: 'body'
      },
      {
        title: 'Author',
        field: 'author'
      }
    ]
  }
}

export default new NoteStore();
