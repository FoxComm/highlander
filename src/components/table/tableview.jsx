'use strict';

import React from 'react';
import Table from './table';
import TablePaginator from './paginator';

export default class TableView extends React.Component {
  constructor(...args) {
    super(...args);
    this.state = {
      start: 0,
      limit: 1
    }
  }

  onLimitChange(event) {
    event.preventDefault();
    this.store.setLimit(+event.target.value);
  }

  render() {
    let showPaginator = this.props.paginator && this.props.store.rows.length > this.state.limit;
    let paginator = showPaginator && (
        <TablePaginator
          start={this.state.start}
          limit={this.state.limit}
          total={100}
          setStart={this.props.store.setStart.bind(this.props.store)}
          />
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

TableView.propTypes = {
  children: React.PropTypes.any,
  start: React.PropTypes.number,
  limit: React.PropTypes.number,
  total: React.PropTypes.number,
  store: React.PropTypes.object,
  renderRow: React.PropTypes.func,
  paginator: React.PropTypes.bool
};

TableView.defaultProps = {
  paginator: true
};
