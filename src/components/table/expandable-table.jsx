// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// components
import TableView from './tableview';
import MultiSelectHead, { selectionState } from './multi-select-head';

export default class ExpandableTable extends React.Component {
  static propTypes = {
    columns: PropTypes.array.isRequired,
    data: PropTypes.shape({
      rows: PropTypes.array,
      total: PropTypes.number,
      from: PropTypes.number,
      size: PropTypes.number,
    }),
    renderRow: PropTypes.func,
    emptyMessage: PropTypes.string.isRequired,
    predicate: PropTypes.func,
    className: PropTypes.string,
  };

  static defaultProps = {
    predicate: entity => entity.id,
  };

  @autobind
  renderRow(row, index) {
    const {renderRow, columns, params} = this.props;

    return renderRow(row, index, columns, params);
  }

  render() {
    return (
      <TableView
        {...this.props}
        className={classNames('fc-expandable-table', this.props.className)}
        columns={this.props.columns}
        renderRow={this.renderRow} />
    );
  }
}
