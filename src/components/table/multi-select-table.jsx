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
  };

  static defaultProps = {
    toggleColumnPresent: true,
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      allChecked: false,
      checkedIds: [],
    };
  }

  getRowSetChecked(key) {
    return (checked) => {
      let {allChecked, checkedIds} = this.state;

      if (allChecked !== checked) {
        checkedIds = checkedIds.concat(key);
      } else {
        checkedIds = _.difference(checkedIds, [key]);
      }

      checkedIds = _.uniq(checkedIds);

      this.setState({checkedIds});
    };
  }

  @autobind
  setAllChecked(checked) {
    //set allChecked flag and reset checkedIds list
    this.setState({allChecked: checked, checkedIds: []});
  }

  @autobind
  setPageChecked(checked) {
    let {allChecked, checkedIds} = this.state;

    //if checked states differ - add id's, else - remove them
    if (allChecked !== checked) {
      checkedIds = checkedIds.concat(this.keys);
    } else {
      checkedIds = _.difference(checkedIds, this.keys);
    }

    checkedIds = _.uniq(checkedIds);

    this.setState({checkedIds});
  }

  get keys() {
    const {data: {rows}, predicate} = this.props;

    return _.uniq(_.map(rows, predicate));
  }

  get checkboxHead() {
    const keys = this.keys;
    const {allChecked, checkedIds} = this.state;
    const {None, Some, All} = selectionState;
    const checkedCount = checkedIds.filter(key => keys.includes(key)).length;

    let pageChecked;
    if (checkedCount === keys.length) {
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
    const {allChecked, checkedIds} = this.state;
    const {renderRow, predicate} = this.props;
    const key = predicate(row);

    return renderRow(row, index, this.columns, {
      checked: allChecked !== checkedIds.includes(key),
      setChecked: this.getRowSetChecked(key),
    });
  }

  render() {
    return (
      <TableView
        {...this.props}
        allChecked={this.state.allChecked}
        checkedIds={this.state.checkedIds}
        className={classNames('fc-multi-select-table', this.props.className)}
        columns={this.columns}
        renderRow={this.renderRow} />
    );
  }
}
