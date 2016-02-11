import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import DropdownItem from './dropdownItem';

class Dropdown extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      open: !!this.props.open,
      selectedValue: '',
    };
  }

  findTitleByValue(value, props = this.props) {
    if (props.items) {
      const item = _.find(props.items, item => item[0] == value);
      return item && item[1];
    } else {
      const item = _.findWhere(React.Children.toArray(props.children), {props: {value: value}});
      return item && item.props.children;
    }
  }

  @autobind
  handleToggleClick(event) {
    event.preventDefault();
    if (this.props.disabled) {
      return;
    }
    this.setState({
      open: !this.state.open
    });
  }

  @autobind
  handleItemClick(value, title) {
    const state = {open: false};
    if (this.props.changeable) {
      state.selectedValue = value;
    }

    this.setState(state, () => {
      if (this.props.onChange) {
        this.props.onChange(value, title);
      }
    });
  }

  get input() {
    const {editable, disabled, placeholder, name, value, renderNullTitle} = this.props;
    const actualValue = this.state.selectedValue || value;
    const title = this.findTitleByValue(actualValue) || renderNullTitle(value, placeholder);

    if (editable) {
      return (
        <div className="fc-dropdown__value">
          <input placeholder={placeholder} disabled={disabled} defaultValue={title} key={actualValue} />
        </div>
      );
    }

    return (
      <div className="fc-dropdown__value">
        {title}
        <input name={name} type="hidden" value={actualValue} />
      </div>
    );
  }

  get dropdownButton() {
    return (
      <div className="fc-dropdown__button"
           disabled={this.props.disabled}
           onClick={this.handleToggleClick}>
        <i className="icon-chevron-down"></i>
      </div>
    );
  }

  @autobind
  onBlur() {
    this.setState({open: false});
  }

  componentWillReceiveProps(newProps) {
    this.setState({
      selectedValue: newProps.value,
    });
  }

  render() {
    const {primary, editable, items, children} = this.props;
    const className = classNames(this.props.className, {
      'fc-dropdown': true,
      '_primary': primary,
      '_editable': editable,
      '_open': this.state.open
    });

    return (
      <div className={className} onBlur={this.onBlur} tabIndex="0">
        <div className="fc-dropdown__controls" onClick={editable ? this.handleToggleClick : null}>
          {this.dropdownButton}
          {this.input}
        </div>
        <ul className="fc-dropdown__items">
          {items && _.map(items, ([value, title]) => (
            <DropdownItem value={value} key={value} onSelect={this.handleItemClick}>
              {title}
            </DropdownItem>
          )) || React.Children.map(children, item => (
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

Dropdown.itemsType = PropTypes.arrayOf(PropTypes.array);

Dropdown.propTypes = {
  name: PropTypes.string,
  className: PropTypes.string,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
  ]),
  disabled: PropTypes.bool,
  editable: PropTypes.bool,
  changeable: PropTypes.bool,
  primary: PropTypes.bool,
  open: PropTypes.bool,
  placeholder: PropTypes.string,
  onChange: PropTypes.func,
  items: Dropdown.itemsType,
  children: PropTypes.node,
  renderNullTitle: PropTypes.func,
};

Dropdown.defaultProps = {
  renderNullTitle: (value, placeholder) => {
    return placeholder;
  },
  changeable: true,
  disabled: false,
};

export default Dropdown;
