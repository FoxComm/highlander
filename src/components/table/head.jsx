import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
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
  renderColumn(column, index) {
    const classnames = classNames({
      'fc-table-th': true,
      'sorting': true,
      'sorting-desc': `${column.field}` === this.props.sortBy,
      'sorting-asc': `-${column.field}` === this.props.sortBy
    });
    return (
      <th className={classnames} key={`${column.field}`} onClick={this.onHeaderClick.bind(this, column.field)}>
        {column.text}
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
