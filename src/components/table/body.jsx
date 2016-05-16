import _ from 'lodash';
import { autobind } from 'core-decorators';
import flatMap from '../../lib/flatMap';
import React, { PropTypes, Component } from 'react';

import TableRow from './row';
import TableCell from './cell';

export default class TableBody extends Component {

  static propTypes = {
    columns: PropTypes.array.isRequired,
    rows: PropTypes.oneOfType([
      PropTypes.array,
      PropTypes.object,
    ]).isRequired,
    renderRow: PropTypes.func,
    predicate: PropTypes.func,
    processRows: PropTypes.func,
    detectNewRows: PropTypes.bool,
    children: PropTypes.node,
  };

  static defaultProps = {
    predicate: entity => entity.id,
    processRows: _.identity,
    detectNewRows: false,
  };

  state = {
    newIds: [],
  };

  @autobind
  defaultRenderRow(row, index, isNew) {
    const rowKey = this.props.predicate && this.props.predicate(row) || index;
    return (
      <TableRow key={`row-${rowKey}`} isNew={isNew}>
        {this.props.columns.map((column, cellIdx) => {
          return (
            <TableCell column={column} key={`cell-${cellIdx}`}>
              {row[column.field]}
            </TableCell>
          );
        })}
      </TableRow>
    );
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.detectNewRows) {
      let newIds = [];

      if (this.props.predicate && nextProps.rows && nextProps.rows !== this.props.rows) {

        const oldRows = _.indexBy(this.props.rows, this.props.predicate);
        const newRows = _.indexBy(nextProps.rows, this.props.predicate);

        newIds = _.difference(_.keys(newRows), _.keys(oldRows));
      }
      this.setState({ newIds });
    }
  }

  get tableRows() {
    const renderRow = this.props.renderRow || this.defaultRenderRow;

    return flatMap(this.props.rows, ((row, index) => {
      const isNew = this.props.detectNewRows &&
                    this.props.predicate &&
                    (this.state.newIds.indexOf(String(this.props.predicate(row))) != -1);

      return renderRow(row, index, isNew);
    }));
  }

  render() {
    const { children } = this.props;
    const rows = _.isEmpty(children) ? this.tableRows : React.Children.toArray(children);
    return (
      <tbody className="fc-table-tbody">
        {this.props.processRows(rows, this.props.columns)}
      </tbody>
    );
  }
}
