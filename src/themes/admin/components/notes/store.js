'use strict';

import TableStore from '../../lib/tableStore';

class NoteStore extends TableStore {
  constructor(props) {
    super(props);
    this.columns = [
      {
        title: 'Date/Time',
        field: 'createdAd'
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

  get baseUri() {
    return `${this.rootUri}/notes`;
  }
}

export default new NoteStore();
