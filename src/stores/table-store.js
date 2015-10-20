'use strict';

import { List, Map } from 'immutable';
import BaseStore from './base-store';
import TableConstants from '../constants/table';

export default class TableStore extends BaseStore {
  constructor() {
    super();
    this.changeEvent = 'change-table';
    this.state = Map({
      columns: List([]),
      rows: List([]),
      startIndex: 0,
      pageSize: 25
    });

    this.bindListener(TableConstants.UPDATE_ROWS, this.handleUpdateRows);
  }

  handleUpdateColumns(action) {
    let state = this.state.set('columns', action.columns);
    this.setState(state);
  }

  handleUpdateRows(action) {
    let state = this.state.set('rows', action.rows);
    this.setState(state);
  }
}