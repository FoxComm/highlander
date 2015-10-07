'use strict';

import React from 'react';
import TableStore from '../../lib/table-store';
import Table from './table';
import TablePaginator from './paginator';

export default class TableView extends React.Component {
  static propTypes = {
    children: React.PropTypes.any,
    store: React.PropTypes.instanceOf(TableStore),
    renderRow: React.PropTypes.func,
    paginator: React.PropTypes.bool
  };

  static defaultProps = {
    paginator: true
  };

  constructor(...args) {
    super(...args);
  }

  componentDidMount() {
    this.props.store.addListener('change', this.forceUpdate.bind(this, null));
  }

  onLimitChange(event) {
    event.preventDefault();
    this.store.setLimit(+event.target.value);
  }

  render() {
    let showPaginator = this.props.paginator && this.props.store.models.length > this.props.store.limit;
    let paginator = showPaginator && (
        <TablePaginator store={this.props.store}/>
      );

    return (
      <div className="fc-tableview">
        {showPaginator && (
          <div className="fc-table-header">
            {paginator}
          </div>
        )}
        <Table store={this.props.store} renderRow={this.props.renderRow}/>
        {showPaginator && (
          <div className="fc-table-footer">
            <select onChange={this.onLimitChange.bind(this)}>
              <option value="10">Show 10</option>
              <option value="25">Show 25</option>
              <option value="50">Show 50</option>
              <option value="100">Show 100</option>
              <option value="Infinity">Show all</option>
            </select>
            {paginator}
          </div>
        )}
      </div>
    );
  }
}
