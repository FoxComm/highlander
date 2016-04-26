// libs
import React, {PropTypes} from 'react';
import {autobind} from 'core-decorators';
import { assoc, dissoc } from 'sprout-data';

// components
import { Checkbox } from '../checkbox/checkbox';

export default class ColumnSelector extends React.Component {
  static propTypes = {
    columns: PropTypes.array.isRequired,
    onChange: PropTypes.func,
  };

  state = {
    selectedColumns: {},
  };

  toggleColumnsSelected(column, id) {
    const selectedColumns = this.state.selectedColumns;

    if (selectedColumns[column.name]) {
      this.setState({
        selectedColumns: dissoc(selectedColumns, column.name)
      });
    } else {
      this.setState({
        selectedColumns: assoc(selectedColumns, column.name, column)
      });
    }
  }

  render() {
    let columnName = this.props.columns.map((item, id) => {
      return (
        <li key={id}>
          <Checkbox id={`choose-column-${id}`} onChange={e => this.toggleColumnsSelected(item, id)}/>
          {item.text}
        </li>
      );
    });

    return (
      <ul>
        {columnName}
      </ul>
    );
  }
}