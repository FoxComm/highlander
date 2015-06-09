'use strict';

import React from 'react';

class TableBody extends React.Component {
  render() {
    let columns = this.props.columns;

    let createRow = function(row, idx) {
      return (
        <tr key={idx}>
          {columns.map(function(column) {
            return <td key={`${idx}-${column.field}`}>{row[column.field]}</td>;
          })}
        </tr>
      );
    };

    return <tbody>{this.props.rows.map(createRow)}</tbody>;
  }
}

TableBody.propTypes = {
  columns: React.PropTypes.array,
  rows: React.PropTypes.array
};

export default TableBody;
