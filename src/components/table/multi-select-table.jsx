import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import TableView from './tableview';
import { Checkbox } from '../checkbox/checkbox';

export default class MultiSelectTable extends React.Component {
  static propTypes = {
    columns: PropTypes.array.isRequired,
    data: PropTypes.shape({
      rows: PropTypes.array,
      total: PropTypes.number,
      from: PropTypes.number,
      size: PropTypes.number
    }),
    renderRow: PropTypes.func,
    setState: PropTypes.func,
    emptyMessage: PropTypes.string.isRequired,
    toggleColumnPresent: PropTypes.bool
  };

  static defaultProps = {
    toggleColumnPresent: true
  };

  get columns() {
    const selectColumn = {
      field: 'selectColumn',
      control: <Checkbox />,
      className: '__select-column'
    };

    const toggleColumn = {
      field: 'toggleColumns',
      icon: 'icon-settings-col',
      className: '__toggle-columns'
    };

    return this.props.toggleColumnPresent ? [
      selectColumn,
      ...this.props.columns,
      toggleColumn
    ] : [
      selectColumn,
      ...this.props.columns
    ];
  }

  @autobind
  renderRow(row, index) {
    return this.props.renderRow(row, index, this.columns);
  }

  render() {
    return (
      <TableView
        className="fc-multi-select-table"
        columns={this.columns}
        data={this.props.data}
        renderRow={this.renderRow}
        setState={this.props.setState}
        showEmptyMessage={true}
        emptyMessage={this.props.emptyMessage}
      />
    );
  }
}
