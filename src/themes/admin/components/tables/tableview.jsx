'use strict';

import React from 'react';
import TableHead from './head';
import TableBody from './body';
import TablePaginator from './paginator';

export default class TableView extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      start: this.props.start,
      limit: this.props.limit
    };
  }

  setStart(value) {
    this.setState({
      start: Math.max(0, Math.min(this.props.rows.length - this.state.limit, value))
    });
  }

  setLimit(value) {
    let limit = Math.max(0, Math.min(this.props.rows.length, value));
    let start = Math.min(this.state.start, this.props.rows.length - limit);
    this.setState({
      start: start,
      limit: limit
    });
  }

  onLimitChange(event) {
    event.preventDefault();
    this.setLimit(+event.target.value);
  }

  render() {
    return (
      <div className="gutter">
        <div className="fc-table-header">
          <TablePaginator
            start={this.state.start}
            limit={this.state.limit}
            total={this.props.rows.length}
            setStart={this.setStart.bind(this)}
            />
        </div>
        <table className='fc-table'>
          <TableHead
            columns={this.props.columns}
            setSorting={this.props.sort}
            />
          <TableBody
            columns={this.props.columns}
            rows={this.props.rows.slice(this.state.start, this.state.start + this.state.limit)}
            model={this.props.model}
            />
        </table>
        <div className="fc-table-footer">
          <select onChange={this.onLimitChange.bind(this)}>
            <option value="10">Show 10</option>
            <option value="25">Show 25</option>
            <option value="50">Show 50</option>
            <option value="100">Show 100</option>
            <option value="Infinity">Show all</option>
          </select>
          <TablePaginator
            start={this.state.start}
            limit={this.state.limit}
            total={this.props.rows.length}
            setStart={this.setStart.bind(this)}
            />
        </div>
      </div>
    );
  }
}

TableView.propTypes = {
  model: React.PropTypes.string,
  columns: React.PropTypes.array,
  rows: React.PropTypes.array,
  start: React.PropTypes.number,
  limit: React.PropTypes.number,
  sort: React.PropTypes.func
};

TableView.defaultProps = {
  start: 0,
  limit: 10
};
