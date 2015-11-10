import TableStore from '../lib/table-store';

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
    ];
  }

  identity(item) {
    return item.id;
  }
}

export default new NoteStore();
