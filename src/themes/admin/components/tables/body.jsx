'use strict';

import React from 'react';
import moment from 'moment';
import { Link } from 'react-router';

export default class TableBody extends React.Component {
  formatCurrency(num) {
    num = num.toString();
    let
      dollars = num.slice(0, -2),
      cents   = num.slice(-2);
    dollars = dollars.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    return `$${dollars}.${cents}`;
  }

  convert(field, column) {
    let model = this.props.model;
    switch(column.type) {
      case 'id': return <Link to={model} params={{order: field}}>{field}</Link>;
      case 'currency': return this.formatCurrency(field);
      case 'date': return moment(field).format(column.format || 'DD/MM/YYYY');
      default: return field;
    }
  }

  findComponent(name, row, field) {
    let
      children  = this.props.children,
      model     = row[field] ? row[field] : row;
    children = Array.isArray(children) ? children : [children];
    let element = children.filter((child) => { return child.type.name === name; })[0];
    return React.cloneElement(element, {model: model});
  }

  render() {
    let columns = this.props.columns;
    let createRow = (row, idx) => {
      return (
        <tr key={idx} className={row.isNew ? 'new' : ''}>
          {columns.map((column) => {
            let data = (
              column.component
                ? this.findComponent(column.component, row, column.field)
                : this.convert(row[column.field], column)
            );
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
  rows: React.PropTypes.array,
  model: React.PropTypes.string,
  children: React.PropTypes.node
};
