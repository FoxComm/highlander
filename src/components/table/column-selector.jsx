// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import {autobind} from 'core-decorators';
import { assoc, dissoc } from 'sprout-data';

// components
import { Checkbox } from '../checkbox/checkbox';

export default class ColumnSelector extends React.Component {
  static propTypes = {
    columns: PropTypes.array.isRequired,
    onChange: PropTypes.func,
    setColumns: PropTypes.func,
  };

  state = {
    selectedColumns: this.props.columns.map(column => {
      return _.assign(column, {isVisible: true});
    }),
  };

  toggleColumnsSelected(column, id) {
    const selectedColumns = this.state.selectedColumns;

    selectedColumns[id].isVisible = !selectedColumns[id].isVisible

    this.setState({
      selectedColumns: selectedColumns
    });
  }

  saveColumns = () => {
    let columns = _.filter(this.state.selectedColumns, {isVisible: true});
    this.props.setColumns(columns);
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