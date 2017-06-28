// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';

// components
import { PartialCheckbox } from 'components/core/checkbox';

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
      <PartialCheckbox
        id="multi-select"
        disabled={disabled}
        checked={pageChecked !== selectionState.None}
        halfChecked={pageChecked === selectionState.Some}
        onChange={this.handleToggle}
      />
    );
  }
}
