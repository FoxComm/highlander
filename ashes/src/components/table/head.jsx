import React from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';

import classNames from 'classnames';
import _ from 'lodash';

import TableRow from './row';

class TableHead extends React.Component {
  static propTypes = {
    columns: PropTypes.array.isRequired,
    setState: PropTypes.func,
    sortBy: PropTypes.string
  };

  onHeaderClick(field, event) {
    event.preventDefault();
    this.props.setState && this.props.setState({
      sortBy: this.props.sortBy === field ? `-${field}` : `${field}`
    });
  }

  @autobind
  renderColumn(column) {
    const {sortBy, setState} = this.props;
    const {field} = column;
    const sortable = column.sortable !== false;

    const className = classNames(column.className, {
      'fc-table-th': true,
      'sorting': sortable && setState,
      'sorting-asc': sortable && `${field}` === sortBy,
      'sorting-desc': sortable && `-${field}` === sortBy
    });

    let contents = null;
    if (!_.isEmpty(column.text)) {
      contents = column.text;
    } else if (!_.isEmpty(column.icon)) {
      contents = <i className={column.icon} />;
    } else if (!_.isEmpty(column.control)) {
      contents = column.control;
    }

    return (
      <th className={className}
          key={`${field}`}
          onClick={sortable ? this.onHeaderClick.bind(this, field) : null}>
        {contents}
        {sortable && (
          <span className="fc-table__sorting">
            <i className="icon-down" />
            <i className="icon-up" />
          </span>
        )}
      </th>
    );
  }

  render() {
    return (
      <thead className="fc-table-head">
        <TableRow>
          {this.props.columns.map(this.renderColumn)}
        </TableRow>
      </thead>
    );
  }
}

export default TableHead;
