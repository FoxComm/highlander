import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import TableRow from './row';

import _ from 'lodash';

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
  renderColumn(column, index) {
    const classnames = classNames(column.className, {
      'fc-table-th': true,
      'sorting': this.props.setState,
      'sorting-desc': `${column.field}` === this.props.sortBy,
      'sorting-asc': `-${column.field}` === this.props.sortBy
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
      <th className={classnames} key={`${column.field}`} onClick={this.onHeaderClick.bind(this, column.field)}>
        {contents}
      </th>
    );
  }

  render() {
    return (
      <thead>
      <TableRow>
        {this.props.columns.map(this.renderColumn)}
      </TableRow>
      </thead>
    );
  }
}

export default TableHead;
