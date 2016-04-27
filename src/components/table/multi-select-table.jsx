// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import localStorage from 'localStorage';

// components
import TableView from './tableview';
import MultiSelectHead, { selectionState } from './multi-select-head';
import ColumnSelector from './column-selector';

export default class MultiSelectTable extends React.Component {
  static propTypes = {
    columns: PropTypes.array.isRequired,
    data: PropTypes.shape({
      rows: PropTypes.array,
      total: PropTypes.number,
      from: PropTypes.number,
      size: PropTypes.number,
    }),
    renderRow: PropTypes.func,
    setState: PropTypes.func,
    emptyMessage: PropTypes.string.isRequired,
    hasActionsColumn: PropTypes.bool,
    predicate: PropTypes.func,
    className: PropTypes.string,
    isLoading: PropTypes.bool,
    failed: PropTypes.bool,
    identifier: PropTypes.string,
  };

  static defaultProps = {
    hasActionsColumn: true,
    predicate: entity => entity.id,
    columns: [],
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      allChecked: false,
      toggledIds: [],
      columns: this.getSelectedColumns(),
    };
  }

  getTableIdentifier() {
    if (!this.props.identifier) {
      return this.props.columns.map(item => {
        return item.text
      }).toString()
    }
    return this.props.identifier;
  }

  getSelectedColumns() {
    let columns = localStorage.getItem(this.getTableIdentifier());
    if(!columns) return this.props.columns;

    columns = JSON.parse(columns);
    return _.filter(columns, {isVisible:true});
  }

  getRowSetChecked(key) {
    return (checked) => {
      let {allChecked, toggledIds} = this.state;

      if (allChecked !== checked) {
        toggledIds = toggledIds.concat(key);
      } else {
        toggledIds = _.difference(toggledIds, [key]);
      }

      toggledIds = _.uniq(toggledIds);

      this.setState({toggledIds});
    };
  }

  @autobind
  setColumnSelected(columns) {
    this.setState({columns});
  }

  @autobind
  setAllChecked(checked) {
    //set allChecked flag and reset toggledIds list
    this.setState({allChecked: checked, toggledIds: []});
  }

  @autobind
  setPageChecked(checked) {
    let {allChecked, toggledIds} = this.state;

    //if checked states differ - add id's, else - remove them
    if (allChecked !== checked) {
      toggledIds = toggledIds.concat(this.currentPageIds);
    } else {
      toggledIds = _.difference(toggledIds, this.currentPageIds);
    }

    toggledIds = _.uniq(toggledIds);

    this.setState({toggledIds});
  }

  get currentPageIds() {
    const {data: {rows}, predicate} = this.props;

    return _.uniq(_.map(rows, predicate));
  }

  get checkboxHead() {
    const {total} = this.props.data;
    const {None, Some, All} = selectionState;
    if (!total) {
      return (
        <MultiSelectHead pageChecked={None} disabled={true} />
      );
    }

    const {allChecked, toggledIds} = this.state;
    const toggled = toggledIds.length;

    let pageChecked;
    if ((allChecked && !toggled) || (!allChecked && toggled === total)) {
      pageChecked = All;
    } else if (toggled) {
      pageChecked = Some;
    } else {
      pageChecked = None;
    }

    return (
      <MultiSelectHead pageChecked={pageChecked}
                       setAllChecked={this.setAllChecked}
                       setPageChecked={this.setPageChecked} />
    );
  }

  get columns() {
    const selectColumn = {
      field: 'selectColumn',
      control: this.checkboxHead,
      className: '__select-column',
      sortable: false,
    };

    const toggleColumn = {
      field: 'toggleColumns',
      control: <ColumnSelector setColumns={this.setColumnSelected} columns={this.props.columns} identifier={this.getTableIdentifier()} />,
      icon: 'icon-settings-col',
      className: '__toggle-columns',
      sortable: false,
    };

    return this.props.hasActionsColumn ? [
      selectColumn,
      ...this.state.columns,
      toggleColumn,
    ] : [
      selectColumn,
      ...this.state.columns,
    ];
  }

  @autobind
  renderRow(row, index, isNew) {
    const {allChecked, toggledIds} = this.state;
    const {renderRow, predicate} = this.props;
    const key = predicate(row);

    return renderRow(row, index, this.columns, {
      checked: allChecked !== _.includes(toggledIds, key),
      setChecked: this.getRowSetChecked(key),
      isNew,
    });
  }

  render() {
    return (
      <TableView
        {...this.props}
        allChecked={this.state.allChecked}
        toggledIds={this.state.toggledIds}
        className={classNames('fc-multi-select-table', this.props.className)}
        columns={this.columns}
        renderRow={this.renderRow} />
    );
  }
}
