// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

// components
import CheckboxDropdown from '../bulk-actions/checkbox-dropdown';
import { DropdownItem } from '../dropdown';

export const selectionState = {
  None: 0,
  Some: 1,
  All: 2,
};

const dropdownItems = [
  ['selectAll', 'Select all'],
  ['selectPage', 'Select current page'],
  ['deselectAll', 'Deselect all'],
  ['deselectPage', 'Deselect current page'],
];

export default class MultiSelectHead extends React.Component {

  static propTypes = {
    pageChecked: PropTypes.oneOf(_.values(selectionState)),
    setAllChecked: PropTypes.func,
    setPageChecked: PropTypes.func,
  };

  @autobind
  handleToggle({target: { checked }}) {
    this.props.setPageChecked(checked);
  }

  @autobind
  handleSelect(key) {
    switch (key) {
      case 'selectAll':
        console.log('select all');
        this.props.setAllChecked(true);
        break;
      case 'selectPage':
        console.log('select page');
        this.props.setPageChecked(true);
        break;
      case 'deselectAll':
        console.log('deselect all');
        this.props.setAllChecked(false);
        break;
      case 'deselectPage':
        console.log('deselect page');
        this.props.setPageChecked(true);
        break;
    }
  }

  render() {
    const {pageChecked} = this.props;

    return (
      <div>
        <CheckboxDropdown checked={pageChecked !== selectionState.None}
                          halfChecked={pageChecked === selectionState.Some}
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
