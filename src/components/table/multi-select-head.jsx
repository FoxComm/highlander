// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

// components
import CheckboxDropdown from '../checkbox/checkbox-dropdown';
import { DropdownItem } from '../dropdown';

export const selectionState = {
  None: 0,
  Some: 1,
  All: 2,
};

const dropdownItems = [
  //['selectAll', 'Select all'],
  ['selectPage', 'Select current page'],
  //['deselectAll', 'Deselect all'],
  ['deselectPage', 'Deselect current page'],
];

export default class MultiSelectHead extends React.Component {

  static propTypes = {
    pageChecked: PropTypes.oneOf(_.values(selectionState)),
    setAllChecked: PropTypes.func,
    setPageChecked: PropTypes.func,
    disabled: PropTypes.bool,
  };

  static defaultProps = {
    disabled: false,
  };

  @autobind
  handleToggle({target: { checked }}) {
    this.props.setPageChecked(checked);
  }

  @autobind
  handleSelect(key) {
    switch (key) {
      case 'selectAll':
        this.props.setAllChecked(true);
        break;
      case 'selectPage':
        this.props.setPageChecked(true);
        break;
      case 'deselectAll':
        this.props.setAllChecked(false);
        break;
      case 'deselectPage':
        this.props.setPageChecked(false);
        break;
    }
  }

  render() {
    const {pageChecked, disabled} = this.props;

    return (
      <div>
        <CheckboxDropdown id="multi-select"
                          checked={pageChecked !== selectionState.None}
                          halfChecked={pageChecked === selectionState.Some}
                          disabled={disabled}
                          onToggle={this.handleToggle}
                          onSelect={this.handleSelect}>
          {dropdownItems.map(([value, title]) => (
            <DropdownItem key={value} value={value}>{title}</DropdownItem>
          ))}
        </CheckboxDropdown>
      </div>
    );
  }
}
