// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import {autobind} from 'core-decorators';
import { assoc, dissoc } from 'sprout-data';
import localStorage from 'localStorage';

// components
import { Checkbox } from '../checkbox/checkbox';

export default class ColumnSelector extends React.Component {
  static propTypes = {
    columns: PropTypes.array.isRequired,
    onChange: PropTypes.func,
    setColumns: PropTypes.func,
    identifier: PropTypes.string,
  };

  state = {
    selectedColumns: this.getSelectedColumns(),
  };

  getSelectedColumns() {
    let columns = localStorage.getItem(this.props.identifier);
    if(columns) {
      columns = JSON.parse(columns);
    } else {
      columns = this.props.columns.map(column => {
        return _.assign(column, {isVisible: true});
      });
    }
    return columns;
  }

  toggleColumnsSelected(column, id) {
    const selectedColumns = this.state.selectedColumns;

    selectedColumns[id].isVisible = !selectedColumns[id].isVisible;

    this.setState({
      selectedColumns: selectedColumns
    });
  }

  @autobind
  saveColumns() {
    let columns = _.filter(this.state.selectedColumns, {isVisible: true});
    this.props.setColumns(columns);
    localStorage.setItem(this.props.identifier, JSON.stringify(this.state.selectedColumns));
  }

  render() {
    let columnName = this.state.selectedColumns.map((item, id) => {

      let checked = item.isVisible;

      return (
        <li key={id}>
          <Checkbox id={`choose-column-${id}`} onChange={e => this.toggleColumnsSelected(item, id)} checked={checked}/>
          {item.text}
        </li>
      );
    });

    return (
      <div>
        <ul>
          {columnName}
        </ul>
        <button onClick={this.saveColumns}>Save</button>
      </div>
    );
  }
}