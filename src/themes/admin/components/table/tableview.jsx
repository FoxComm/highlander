'use strict';

import React from 'react';
import TableHead from './head';
import TableBody from './body';
import TablePaginator from './paginator';

export default class TableView extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      start: 1,
      end: 10,
      total: 43
    }
  }

  render() {
    return (
      <div className="fc-table-view">
        <div className="fc-table-header">
          <div className="fc-table-header-right">
            <TablePaginator start={this.state.start} end={this.state.end} total={this.state.total}/>
          </div>
        </div>
        <table className='fc-table'>
          <TableHead columns={this.props.columns}/>
          <TableBody columns={this.props.columns} rows={this.props.rows}/>
        </table>
        <div className="fc-table-footer">
          footer
        </div>
      </div>
    );
  }
}

TableView.propTypes = {
  columns: React.PropTypes.array,
  rows: React.PropTypes.array
};
