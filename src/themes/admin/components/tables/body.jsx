'use strict';

import React from 'react';
import moment from 'moment';
import { Link } from 'react-router';
import { dispatch } from '../../lib/dispatcher';

export default class TableBody extends React.Component {
  formatCurrency(num) {
    num = num.toString();
    let
      dollars = num.slice(0, -2),
      cents   = num.slice(-2);
    dollars = dollars.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    return `$${dollars}.${cents}`;
  }

  convert(field, column, model) {
    switch(column.type) {
      case 'id': return <Link to={model} params={{order: field}}>{field}</Link>;
      case 'currency': return this.formatCurrency(field);
      case 'date': return moment(field).format('DD/MM/YYYY');
      case 'dispatch': return <a className='btn' onClick={dispatch.bind(null, column.event, field)}>{column.text}</a>;
      default: return field;
    }
  }

  render() {
    let columns = this.props.columns;

    let createRow = (row, idx) => {
      return (
        <tr key={idx}>
          {columns.map((column) => {
            let data = this.convert(row[column.field], column, this.props.model);
            return <td key={`${idx}-${column.field}`}>{data}</td>;
          })}
        </tr>
      );
    };

    return <tbody>{this.props.rows.map(createRow)}</tbody>;
  }
}

TableBody.contextTypes = {
  router: React.PropTypes.func
};

TableBody.propTypes = {
  columns: React.PropTypes.array,
  rows: React.PropTypes.array,
  model: React.PropTypes.string
};
