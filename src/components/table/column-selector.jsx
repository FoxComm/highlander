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
    //this.moveItem = this.moveItem.bind(this);
    this.state = {
      selectedColumns: this.getSelectedColumns(),
      isSelectorVisible: false,
    };
  }

  @autobind
  moveItem(dragIndex, hoverIndex) {
    const { selectedColumns } = this.state;
    const dragItem = selectedColumns[dragIndex];

    this.setState(update(this.state, {
      selectedColumns: {
        $splice: [
          [dragIndex, 1],
          [hoverIndex, 0, dragItem]
        ]
      }
    }));
  }

  getSelectedColumns() {
    let columns = localStorage.getItem(this.props.identifier);
    if(columns) {
      columns = JSON.parse(columns);
    } else {
      columns = this.props.columns.map((column, i) => {
        return _.assign(column, {
          isVisible: true,
          id: i,
        });
      });
    }
    return columns;
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
    let columns = _.filter(this.state.selectedColumns, {isVisible: true});
    this.props.setColumns(columns);
    localStorage.setItem(this.props.identifier, JSON.stringify(this.state.selectedColumns));
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
            checked={checked}
            onChange={e => this.toggleColumnSelection(id)}>
        </SelectorItem>
      );
    });
  }

  render() {
    return (
      <div styleName="column-selector">
        <i className="icon-settings-col" onClick={this.toggleColumnSelector}/>
        {this.state.isSelectorVisible && <Overlay shown={true} onClick={this.toggleColumnSelector}/> }
        {this.state.isSelectorVisible && (
          <div styleName="dropdown">
            <ul styleName="list">
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