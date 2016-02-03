// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import { HalfCheckbox } from './checkbox';
import { DecrementButton } from '../common/buttons';

export default class CheckboxDropdown extends React.Component {

  static propTypes = {
    name: PropTypes.string,
    className: PropTypes.string,
    checked: PropTypes.bool,
    halfChecked: PropTypes.bool,
    onToggle: PropTypes.func,
    onSelect: PropTypes.func,
    children: PropTypes.node,
  };

  static defaultProps = {
    checked: false,
    halfChecked: false,
    onSelect: _.noop,
  };

  constructor(...args) {
    super(...args);
    this.state = {
      open: false,
    };
  }

  @autobind
  handleToggleClick(event) {
    event.preventDefault();
    this.setState({
      open: !this.state.open
    });
  }

  @autobind
  handleItemClick(value) {
    this.setState({
      open: false,
    }, () => {
      this.props.onSelect(value);
    });
  }

  @autobind
  onBlur() {
    setTimeout(() => this.setState({open: false}), 0);
  }

  render() {
    const {checked, halfChecked, onToggle, children} = this.props;
    const className = classNames(
      this.props.className,
      'fc-dropdown',
      {'_open': this.state.open},
    );

    return (
      <div className={className} onBlur={this.onBlur} tabIndex="0">
        <HalfCheckbox inline={true}
                      docked="left"
                      checked={checked}
                      halfChecked={halfChecked}
                      onChange={onToggle} />
        <DecrementButton docked="right"
                         className="_small"
                         onClick={this.handleToggleClick} />
        <ul className="fc-dropdown__items">
          {React.Children.map(children, item => (
              React.cloneElement(item, {
                onSelect: this.handleItemClick,
              })
            )
          )}
        </ul>
      </div>
    );
  }
}
