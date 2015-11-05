'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import TableRow from './row';
import TableCell from './cell';

export default class TableBody extends React.Component {

  static propTypes = {
    columns: PropTypes.array.isRequired,
    rows: PropTypes.array.isRequired,
    renderRow: PropTypes.func,
    predicate: PropTypes.func,
    processRows: PropTypes.func,
    detectNewRows: PropTypes.bool
  };

  static defaultProps = {
    predicate: entity => entity.id,
    processRows: _.identity,
    detectNewRows: false
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      newIds: []
    };
  }

  defaultRenderRow(row, index, isNew) {
    return (
      <TableRow isNew={isNew}>
        {this.props.columns.map((column) => <TableCell>{row[column.field]}</TableCell>)}
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

    return _.flatten(this.props.rows.map((row, index) => {
      const isNew = this.props.detectNewRows && this.props.predicate && (this.state.newIds.indexOf(String(this.props.predicate(row))) != -1);

      return renderRow(row, index, isNew);
    }));
  }

  render() {
    return (
      <tbody className="fc-table-tbody">
        {this.props.processRows(this.tableRows)}
      </tbody>
    );
  }
}
