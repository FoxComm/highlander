// libs
import React from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import _ from 'lodash';

// components
import TableRow from './row';
import Icon from 'components/core/icon';

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
      contents = <Icon name={column.icon} />;
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
            <Icon name="down" />
            <Icon name="up" />
          </span>
        )}
      </th>
    );
  }

  render() {
    const { getRef } = this.props;

    return (
      <thead className="fc-table-head" ref={getRef}>
        <TableRow>
          {this.props.columns.map(this.renderColumn)}
        </TableRow>
      </thead>
    );
  }
}

export default TableHead;
