// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

// components
import TableView from './tableview';
import MultiSelectHead, { selectionState } from './multi-select-head';

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
    toggleColumnPresent: PropTypes.bool,
    predicate: PropTypes.func,
  };

  static defaultProps = {
    toggleColumnPresent: true,
    predicate: entity => entity.id,
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      allChecked: false,
      toggledIds: [],
    };
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
    const currentPageIds = this.currentPageIds;
    const {allChecked, toggledIds} = this.state;
    const {None, Some, All} = selectionState;
    const checkedCount = toggledIds.filter(key => currentPageIds.includes(key)).length;

    let pageChecked;
    if (checkedCount === currentPageIds.length) {
      pageChecked = allChecked ? None : All;
    } else if (checkedCount > 0) {
      pageChecked = Some;
    } else {
      pageChecked = allChecked ? All : None;
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
      icon: 'icon-settings-col',
      className: '__toggle-columns',
    };

    return this.props.toggleColumnPresent ? [
      selectColumn,
      ...this.props.columns,
      toggleColumn,
    ] : [
      selectColumn,
      ...this.props.columns,
    ];
  }

  @autobind
  renderRow(row, index) {
    const {allChecked, toggledIds} = this.state;
    const {renderRow, predicate} = this.props;
    const key = predicate(row);

    return renderRow(row, index, this.columns, {
      checked: allChecked !== toggledIds.includes(key),
      setChecked: this.getRowSetChecked(key),
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
