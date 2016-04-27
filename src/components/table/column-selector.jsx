// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import {autobind} from 'core-decorators';
import { assoc, dissoc } from 'sprout-data';
import localStorage from 'localStorage';

// components
import { Checkbox } from '../checkbox/checkbox';
import { PrimaryButton } from '../common/buttons';

//styles
import styles from './column-selector.css';

export default class ColumnSelector extends React.Component {
  static propTypes = {
    columns: PropTypes.array.isRequired,
    onChange: PropTypes.func,
    setColumns: PropTypes.func,
    identifier: PropTypes.string,
    toggleColumnSelector: PropTypes.func,
  };

  state = {
    selectedColumns: this.getSelectedColumns(),
  };

  componentDidMount() {
    window.addEventListener('click', this.props.toggleUserMenu, false);
  }

  componentWillUnmount() {
    window.removeEventListener('click', this.props.toggleUserMenu, false);
  }

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

  renderSelectorItems() {
    return this.state.selectedColumns.map((item, id) => {
      let checked = item.isVisible;

      return (
        <li key={id}>
          <Checkbox id={`choose-column-${id}`} onChange={e => this.toggleColumnsSelected(item, id)} checked={checked}>
            {item.text}
          </Checkbox>
        </li>
      );
    });
  }

  render() {
    return (
      <div styleName="column-selector">
        <ul styleName="list">
          {this.renderSelectorItems()}
        </ul>
        <div styleName="actions">
          <PrimaryButton onClick={this.saveColumns}>
            Save
          </PrimaryButton>
        </div>
      </div>
    );
  }
}