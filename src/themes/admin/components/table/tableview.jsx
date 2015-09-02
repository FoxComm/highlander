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

  setStart(start) {
    this.setState({
      start: Math.max(0, Math.min(this.props.rows.length - this.state.limit, start))
    });
  }

  setLimit(limit) {
    this.setState({
      limit: Math.max(0, Math.min(this.props.rows.length, limit))
    });
  }

  render() {
    return (
      <div className="fc-table-view gutter">
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
            />
          <TableBody
            columns={this.props.columns}
            rows={this.props.rows.slice(this.state.start, this.state.start + this.state.limit)}
            model={this.props.model}
            />
        </table>
        <div className="fc-table-footer">
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
  limit: React.PropTypes.number
};

TableView.defaultProps = {
  start: 0,
  limit: 10
};
