'use strict';

import React, { PropTypes } from 'react';
import TableHead from './head';
import TableBody from './body';
import TablePaginator from './paginator';

export default class TableView extends React.Component {

  static propTypes = {
    model: PropTypes.string,
    columns: PropTypes.array,
    rows: PropTypes.array,
    start: PropTypes.number,
    limit: PropTypes.number,
    sort: PropTypes.func,
    paginator: PropTypes.bool,
    children: PropTypes.node
  };

  static defaultProps = {
    start: 0,
    limit: 25,
    paginator: true
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      start: this.props.start,
      limit: this.props.paginator ? this.props.limit : Infinity
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
    let showPaginator = false;
    let paginator = null;

    return (
      <div className="fc-tableview">
        {showPaginator && (
          <div className="fc-table-header">
            {paginator}
          </div>
        )}
        <table className='fc-table'>
          <TableHead
            columns={this.props.columns}
            setSorting={this.props.sort}
            />
          <TableBody
            columns={this.props.columns}
            rows={this.props.rows.slice(this.state.start, this.state.start + this.state.limit)}
            model={this.props.model}
            >
            {this.props.children}
          </TableBody>
        </table>
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
