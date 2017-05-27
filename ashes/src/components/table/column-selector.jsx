/**
 * @flow weak
 */

// libs
import _ from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React from 'react';
import { DragDropContext } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import localStorage from 'localStorage';

// components
import { PrimaryButton } from 'components/core/button';
import Dropdown from '../dropdown/generic-dropdown';
import SelectorItem from './column-selector-item';

//styles
import styles from './column-selector.css';

type Props = {
  columns: Array<Object>,
  onChange: Function,
  setColumns: Function,
  identifier: string,
  toggleColumnsBtn?: boolean,
};

type State = {
  selectedColumns: Array<Object>,
  hasDraggingItem: boolean,
};

/*::`*/
@DragDropContext(HTML5Backend) /*::`;*/
export default class ColumnSelector extends React.Component {
  props: Props;

  state: State = {
    selectedColumns: this.getSelectedColumns(),
    hasDraggingItem: false,
  };

  _dropdown: any;

  @autobind
  moveItem(dragIndex: number, hoverIndex: number) {
    let { selectedColumns } = this.state;
    const dragItem = selectedColumns[dragIndex];

    selectedColumns.splice(dragIndex, 1);
    selectedColumns.splice(hoverIndex, 0, dragItem);

    this.setState({
      selectedColumns: selectedColumns,
      hasDraggingItem: true
    });
  }

  @autobind
  dropItem() {
    this.setState({
      hasDraggingItem: false
    });
  }

  getSelectedColumns() {
    let columns = localStorage.getItem('columns');

    if (columns) {
      columns = JSON.parse(columns);
      if (columns[this.props.identifier]) return columns[this.props.identifier];
    }

    return this.props.columns.map((column, i) => {
      return _.assign(column, {
        isVisible: true,
        id: i,
      });
    });
  }

  toggleColumnSelection(id: any) {
    let selectedColumns = this.state.selectedColumns;

    selectedColumns[id].isVisible = !selectedColumns[id].isVisible;

    this.setState({
      selectedColumns: selectedColumns,
    });
  }

  @autobind
  saveColumns() {
    const tableName = this.props.identifier;
    const columnState = this.state.selectedColumns;

    const filteredColumns = _.filter(columnState, { isVisible: true });

    if (filteredColumns.length > 0) {
      // update table data
      this.props.setColumns(filteredColumns);

      // save to storage
      let columns = localStorage.getItem('columns') ? JSON.parse(localStorage.getItem('columns')) : {};
      columns[tableName] = columnState;
      localStorage.setItem('columns', JSON.stringify(columns));

      // close dropdown
      this._dropdown.closeMenu();
    }
  }

  renderSelectorItems() {
    return this.state.selectedColumns.map((item, id) => {
      const checked = item.isVisible;

      return (
        <SelectorItem key={item.id}
                      index={id}
                      id={item.id}
                      text={item.text}
                      moveItem={this.moveItem}
                      dropItem={this.dropItem}
                      checked={checked}
                      onChange={e => this.toggleColumnSelection(id)} />
      );
    });
  }

  @autobind
  renderActions() {
    const filteredColumns = _.filter(this.state.selectedColumns, { isVisible: true });

    return (
      <div styleName="actions">
        <PrimaryButton onClick={this.saveColumns} disabled={!filteredColumns.length}>
          Save
        </PrimaryButton>
      </div>
    );
  }

  render() {
    return (
      <div styleName="column-selector">
        <Dropdown
          className={styles.dropdown}
          listClassName={classNames(styles.list, { [styles._hasDraggingItem]: this.state.hasDraggingItem })}
          changeable={false}
          inputFirst={false}
          dropdownProps={{ icon: 'settings-col' }}
          renderAppend={this.renderActions}
          ref={d => this._dropdown = d}
          toggleColumnsBtn={this.props.toggleColumnsBtn}
          buttonClassName={styles.button}
        >
          {this.renderSelectorItems()}
        </Dropdown>
      </div>
    );
  }
}
