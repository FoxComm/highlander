// libs
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import TableView from './tableview';

export default class ExpandableTable extends Component {

  static propTypes = {
    columns: PropTypes.array.isRequired,
    data: PropTypes.shape({
      rows: PropTypes.array,
      total: PropTypes.number,
      from: PropTypes.number,
      size: PropTypes.number,
    }),
    renderRow: PropTypes.func.isRequired,
    renderDrawer: PropTypes.func.isRequired,
    emptyMessage: PropTypes.string,
    errorMessage: PropTypes.string,
    className: PropTypes.string,
    idField: PropTypes.string.isRequired,
    isLoading: PropTypes.bool,
    failed: PropTypes.bool,
  };

  state = {
    expandedRows: {},
  };

  toggleExpanded(id) {
    const state = this.state.expandedRows[id];

    this.setState({
      expandedRows: {
        ...this.state.expandedRows,
        [id]: !state,
      },
    });
  }

  @autobind
  renderRow(row, index) {
    const { props } = this;

    const {renderRow, renderDrawer, columns, idField} = props;
    const id = _.get(row, idField, '').toString().replace(/ /g,'-');
    const state = !!this.state.expandedRows[id];

    const params = {
      toggleDrawerState: () => this.toggleExpanded(id),
      isOpen: state,
      colSpan: columns.length,
    };

    return [
      renderRow(row, index, columns, params),
      renderDrawer(row, index, params),
    ];
  }

  render() {
    const { props } = this;

    return (
      <TableView
        {...props}
        className={classNames('fc-expandable-table', props.className)}
        columns={props.columns}
        renderRow={this.renderRow}
      />
    );
  }
}
