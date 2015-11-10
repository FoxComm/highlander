import TableStore from '../lib/table-store';

class CustomerStore extends TableStore {
  get baseUri() { return '/customers'; }

  constructor(...args) {
    super(...args);
    this.columns = [
      {
        field: 'name',
        title: 'Name'
      },
      {
        field: 'email',
        title: 'Email'
      },
      {
        field: 'id',
        title: 'Customer ID'
      },
      {
        field: 'shipRegion',
        title: 'Ship To Region'
      },
      {
        field: 'billRegion',
        title: 'Bill To Region'
      },
      {
        field: 'rank',
        title: 'Rank'
      },
      {
        field: 'createdAt',
        title: 'Date/Time Joined'
      }
    ];
  }

  identity(item) {
    return item.id;
  }
}

let customerStore = new CustomerStore();
export default customerStore;
