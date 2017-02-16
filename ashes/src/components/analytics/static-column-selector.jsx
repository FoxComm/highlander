/**
 * @flow weak
 */

// libs
import _ from 'lodash';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import localStorage from 'localStorage';

// components
import { PrimaryButton } from '../common/buttons';
import Dropdown from '../dropdown/generic-dropdown';
import StaticColumnSelectorItem from './static-column-selector-item';

//styles
import styles from './static-column-selector.css';

// types
type Props = {
  columns: Array<Object>,
  onChange: Function,
  setColumns: Function,
  identifier: string,
  actionButtonText?: string,
  dropdownTitle?: string,
}

type State = {
  selectedColumns: Array<Object>,
}

export default class StaticColumnSelector extends React.Component {

  props: Props;

  static defaultProps = {
    actionButtonText: 'Save',
    dropdownTitle: 'Options',
  };

  state: State = {
    selectedColumns: this.getSelectedColumns(),
  };

  _dropdown: any;

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
    const { identifier, setColumns } = this.props;
    const columnState = this.state.selectedColumns;

    const filteredColumns = _.filter(columnState, { isVisible: true });

    if (!_.isNil(filteredColumns) && !_.isEmpty(filteredColumns) && !_.isNil(setColumns)) {
      // update table data
      setColumns(filteredColumns);

      // save to storage
      let columns = localStorage.getItem('columns') ? JSON.parse(localStorage.getItem('columns')) : {};
      columns[identifier] = columnState;
      localStorage.setItem('columns', JSON.stringify(columns));
    }

    // close dropdown
    this._dropdown.closeMenu();
  }

  renderSelectorItems() {
    return this.state.selectedColumns.map((item, id) => {
      const checked = item.isVisible;

      return (
        <StaticColumnSelectorItem
          key={item.id}
          index={id}
          id={item.id}
          text={item.text}
          checked={checked}
          onChange={e => this.toggleColumnSelection(id)} />
      );
    });
  }

  @autobind
  renderActions() {
    const { actionButtonText } = this.props;
    const filteredColumns = _.filter(this.state.selectedColumns, { isVisible: true });

    return (
      <div styleName="actions">
        <PrimaryButton onClick={this.saveColumns} disabled={!filteredColumns.length}>
          {actionButtonText}
        </PrimaryButton>
      </div>
    );
  }

  render() {
    const { dropdownTitle } = this.props;

    return (
      <div styleName="column-selector">
        <Dropdown
          className={styles.dropdown}
          listClassName={classNames(styles.list)}
          placeholder={dropdownTitle}
          changeable={false}
          inputFirst={true}
          dropdownProps={{}}
          renderAppend={this.renderActions}
          ref={d => this._dropdown = d}
        >
          {this.renderSelectorItems()}
        </Dropdown>
      </div>
    );
  }
}
