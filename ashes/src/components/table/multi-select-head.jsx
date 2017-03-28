// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

// components
import { HalfCheckbox } from '../checkbox/checkbox';

export const selectionState = {
  None: 0,
  Some: 1,
  All: 2,
};

export default class MultiSelectHead extends React.Component {

  static propTypes = {
    pageChecked: PropTypes.oneOf(_.values(selectionState)),
    setAllChecked: PropTypes.func,
    setPageChecked: PropTypes.func, // called when checkbox checked (true) or unchecked (false)
    disabled: PropTypes.bool,
  };

  static defaultProps = {
    disabled: false,
  };

  @autobind
  handleToggle({ target: { checked } }) {
    this.props.setPageChecked(checked);
  }

  render() {
    const { pageChecked, disabled } = this.props;

    return (
      <HalfCheckbox
        inline={true}
        id="multi-select"
        disabled={disabled}
        checked={pageChecked !== selectionState.None}
        halfChecked={pageChecked === selectionState.Some}
        onChange={this.handleToggle}
      />
    );
  }
}
