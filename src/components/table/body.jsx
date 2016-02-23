import _ from 'lodash';
import { autobind } from 'core-decorators';
import flatMap from 'lodash.flatmap';
import React, { PropTypes } from 'react';

import TableRow from './row';
import TableCell from './cell';
import WaitAnimation from '../common/wait-animation';

export default class TableBody extends React.Component {

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
    emptyMessage: PropTypes.string,
    isLoading: PropTypes.bool,
  };

  static defaultProps = {
    predicate: entity => entity.id,
    processRows: _.identity,
    detectNewRows: false,
    emptyMessage: '',
    isLoading: false,
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      newIds: []
    };
  }

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

  get emptyMessage() {
    return (
      <tr>
        <td colSpan={this.props.columns.length}>
          <div className="fc-content-box__empty-row">
            {this.props.emptyMessage}
          </div>
        </td>
      </tr>
    );
  }

  get loadingAnimation() {
    return (
      <tr>
        <td colSpan={this.props.columns.length}>
          <div className="fc-content-box__empty-row">
            <WaitAnimation />
          </div>
        </td>
      </tr>
    );
  }

  get tableRows() {
    if (this.props.isLoading) {
      return this.loadingAnimation;
    } else if (_.isEmpty(this.props.rows) && this.props.emptyMessage) {
      return this.emptyMessage;
    }

    const renderRow = this.props.renderRow || this.defaultRenderRow;

    return flatMap(this.props.rows, ((row, index) => {
      const isNew = this.props.detectNewRows &&
                    this.props.predicate &&
                    (this.state.newIds.indexOf(String(this.props.predicate(row))) != -1);

      return renderRow(row, index, isNew);
    }));
  }

  render() {
    return (
      <tbody className="fc-table-tbody">
        {this.props.processRows(this.tableRows, this.props.columns)}
      </tbody>
    );
  }
}
