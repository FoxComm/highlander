// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import { connect } from 'react-redux';

// components
import TableView from './tableview';
import MultiSelectHead, { selectionState } from './multi-select-head';

// redux
import * as ExtandableTableActions from '../../modules/expandable-tables';

@connect((state, props) => ({
  tableState: _.get(state, ['expandableTables', props.entity.entityType, props.entity.entityId], {})
}), {...ExtandableTableActions})
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
    className: PropTypes.string,
    params: PropTypes.object,
    tableState: PropTypes.object,
    idField: PropTypes.string.isRequired,
  };

  @autobind
  renderRow(row, index) {
    const {renderRow, entity, columns, params, tableState, idField, toggleDrawerState} = this.props;

    const id = _.get(row, idField).toString().replace(/ /g,'-');
    const state = _.get(tableState, [id], false);

    const hackedParams = {
      ...params,
      toggleDrawerState: () => toggleDrawerState(entity, id),
      isOpen: state,
    };

    return renderRow(row, index, columns, hackedParams);
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
