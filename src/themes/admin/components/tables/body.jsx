'use strict';

import React from 'react';
import moment from 'moment';
import { Link } from 'react-router';
import { formatCurrency } from '../../lib/format';
import OrderStore from '../orders/store';

export default class TableBody extends React.Component {
  convert(field, column) {
    let model = this.props.model;
    switch(column.type) {
      case 'id': return <Link to={model} params={{order: field}}>{field}</Link>;
      case 'image': return <img src={field}/>;
      case 'currency': return formatCurrency(field);
      case 'date': return <time dateTime={field}>{moment(field).format('MM/DD/YYYY HH:mm:ss')}</time>;
      case 'orderStatus': return OrderStore.statuses[field];
      default: return typeof field === 'object' ? this.displayObject(field) : field;
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

  displayObject(obj) {
    let divs = [];
    for (let field in obj) {
      if (field !== 'createdAt' && typeof obj[field] !== 'object') {
        divs.push(<div key={field}>{obj[field]}</div>);
      }
    }
    return divs;
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
            return <td key={`${idx}-${column.field}`} className={column.field}><div>{data}</div></td>;
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
  model: React.PropTypes.string,
  children: React.PropTypes.node
};
