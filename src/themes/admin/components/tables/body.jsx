'use strict';

import React from 'react';
import moment from 'moment';

function formatCurrency(num) {
  num = num.toString();
  let
    dollars = num.slice(0, -2),
    cents   = num.slice(-2);
  dollars = dollars.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return `$${dollars}.${cents}`;
}

class TableBody extends React.Component {
  render() {
    let columns = this.props.columns;

    function convert(field, type) {
      switch(type) {
        case 'currency': return formatCurrency(field);
        case 'date': return moment(field).format('DD/MM/YYYY');
        default: return field;
      }
    }

    let createRow = function(row, idx) {
      return (
        <tr key={idx}>
          {columns.map(function(column) {
            let data = convert(row[column.field], column.type);
            return <td key={`${idx}-${column.field}`}>{data}</td>;
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
