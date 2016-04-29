// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import {autobind} from 'core-decorators';
import { DragDropContext } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import update from 'react/lib/update';
import localStorage from 'localStorage';

// components
import { PrimaryButton } from '../common/buttons';
import Overlay from '../overlay/overlay';
import SelectorItem from './column-selector-item';

//styles
import styles from './column-selector.css';

@DragDropContext(HTML5Backend)
export default class ColumnSelector extends React.Component {
  static propTypes = {
    columns: PropTypes.array.isRequired,
    onChange: PropTypes.func,
    setColumns: PropTypes.func,
    identifier: PropTypes.string,
    toggleColumnSelector: PropTypes.func,
  };

  constructor(props) {
    super(props);
    this.state = {
      selectedColumns: this.getSelectedColumns(),
      isSelectorVisible: false,
      hasDraggingItem: false,
    };
  }

  @autobind
  moveItem(dragIndex, hoverIndex) {
    const { selectedColumns } = this.state;
    const dragItem = selectedColumns[dragIndex];

    this.setState({
      hasDraggingItem: true
    });
    this.setState(update(this.state, {
      selectedColumns: {
        $splice: [
          [dragIndex, 1],
          [hoverIndex, 0, dragItem]
        ]
      }
    }));
  }

  @autobind
  dropItem() {
    this.setState({
      hasDraggingItem: false
    });
  }

  getSelectedColumns() {
    let columns = localStorage.getItem('columns');

    if(columns) {
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

  toggleColumnSelection(id) {
    let selectedColumns = this.state.selectedColumns;

    selectedColumns[id].isVisible = !selectedColumns[id].isVisible;

    this.setState({
      selectedColumns: selectedColumns,
    });
  }

  @autobind
  saveColumns() {
    let tableName = this.props.identifier;
    let columnState = this.state.selectedColumns;

    // update table data
    let filteredColumns = _.filter(columnState, {isVisible: true});
    this.props.setColumns(filteredColumns);

    // save to storage
    let columns = localStorage.getItem('columns') ? JSON.parse(localStorage.getItem('columns')) : {};
    columns[tableName] = columnState;
    localStorage.setItem('columns', JSON.stringify(columns));

    // close dropdown
    this.toggleColumnSelector();
  }

  @autobind
  toggleColumnSelector() {
    this.setState({
      isSelectorVisible: !this.state.isSelectorVisible,
    });
  }

  renderSelectorItems() {
    return this.state.selectedColumns.map((item, id) => {
      let checked = item.isVisible;

      return (
        <SelectorItem key={item.id}
            index={id}
            id={item.id}
            text={item.text}
            moveItem={this.moveItem}
            dropItem={this.dropItem}
            checked={checked}
            onChange={e => this.toggleColumnSelection(id)}>
        </SelectorItem>
      );
    });
  }

  render() {
    let listClassName = this.state.hasDraggingItem ? '_hasDraggingItem' : '';
    return (
      <div styleName="column-selector">
        <i className="icon-settings-col" onClick={this.toggleColumnSelector}/>
        {this.state.isSelectorVisible && <Overlay shown={true} onClick={this.toggleColumnSelector}/> }
        {this.state.isSelectorVisible && (
          <div styleName="dropdown">
            <ul styleName="list" className={listClassName}>
              {this.renderSelectorItems()}
            </ul>
            <div styleName="actions">
              <PrimaryButton onClick={this.saveColumns}>
                Save
              </PrimaryButton>
            </div>
          </div>
        )}
      </div>
    );
  }
}