// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

// components
import TableView from './tableview';
import { HalfCheckbox } from '../checkbox/checkbox';

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
    toggleColumnPresent: PropTypes.bool,
  };

  static defaultProps = {
    toggleColumnPresent: true,
  };

  constructor(props, context) {
    super(props, context);
    this.state = {};
  }

  getSetRowState(key) {
    return (patch) => {
      const states = this.state;
      const state = states[key];

      this.setState({
        ...states,
        [key]: {
          ...state,
          ...patch
        }
      });
    };
  }

  get checkboxHead() {
    const {state} = this;
    const {data: {rows}, predicate} = this.props;

    //get ids of all currently displayed rows
    const keys = _.uniq(_.map(rows, predicate));
    const checkedCount = _.filter(state, (state, key)=> keys.includes(key) && state.checked).length;
    const handleChange = ({target: { checked }}) => {
      let patch = {};

      keys.forEach((key) => { patch[key] = { checked }; });

      this.setState(patch);
    };

    return (
      <HalfCheckbox checked={checkedCount>0}
                    halfChecked={checkedCount<keys.length}
                    onChange={handleChange} />
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
    const {renderRow, predicate} = this.props;
    const key = predicate(row);

    return renderRow(row, index, this.columns, {
      rowState: this.state[key] || {},
      setRowState: this.getSetRowState(key),
    });
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
        predicate={this.props.predicate}
        emptyMessage={this.props.emptyMessage}
        />
    );
  }
}
