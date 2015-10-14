'use strict';

import React from 'react';

export default class SimpleTable extends React.Component {
  static propTypes = {
    rows: React.PropTypes.any,
    columns: React.PropTypes.any
  };

  renderHeader(columns) {
    return (
      <tr className='fc-table-thead'>
        <th className='fc-table-th'>Method</th>
        <th className='fc-table-th'>Price</th>
      </tr>
    );
  }

  renderRows(rows) {
    let renderRows = [];
    rows.forEach((row, index) => {
        renderRows.push(
          <tr className='fc-table-thead'>
            <td>{ row.get('name') }</td>
            <td>{ row.get('price') }</td>
          </tr>
        );
      }
    );

    return renderRows;
  }

  render() {
    return (
      <table className='fc-table'>
        <thead className='fc-table-thead'>
          { this.renderHeader(this.props.columns) }
        </thead>
        <tbody className='fc-table-tbody'>
          { this.renderRows(this.props.rows) }
        </tbody>
      </table> 
    );
  }
}