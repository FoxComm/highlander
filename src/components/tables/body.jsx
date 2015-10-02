'use strict';

import React from 'react';
import moment from 'moment';
import { Link } from '../link';
import { formatCurrency } from '../../lib/format';
import OrderStore from '../../stores/orders';

export default class TableBody extends React.Component {
  convert(field, column, row) {
    let model = column.model || row.model || this.props.model;
    switch(column.type) {
      case 'id': {
        let params = {};
        params[model] = field;
        return <Link to={model} params={params}>{field}</Link>;
      }
      case 'link': {
        let params = {};
        params[column.model] = row[column.id];
        return <Link to={column.model} params={params}>{field}</Link>;
      }
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
                : this.convert(row[column.field], column, row)
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
  rows: React.PropTypes.array.isRequired,
  model: React.PropTypes.string,
  children: React.PropTypes.node
};
