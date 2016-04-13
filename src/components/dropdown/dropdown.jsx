import _ from 'lodash';
import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import DropdownItem from './dropdownItem';

class Dropdown extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      open: !!this.props.open,
      dropup: false,
      selectedValue: '',
    };
  }

  findTitleByValue(value, props = this.props) {
    if (props.items) {
      const item = _.find(props.items, item => item[0] == value);
      return item && item[1];
    } else {
      const item = _.findWhere(React.Children.toArray(props.children), { props: { value: value } });
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

  componentDidUpdate() {
    const { open } = this.state;

    if (open && !this.isDownVisible && !this.state.dropup) {
      this.setState({ dropup: true });
    }
    if (!open && this.state.dropup) {
      this.setState({ dropup: false });
    }
  }

  get isDownVisible() {
    const dropdown = ReactDOM.findDOMNode(this.refs.items);
    const { left, right, bottom } = dropdown.getBoundingClientRect();

    const leftBottomElement = document.elementFromPoint(left + 1, bottom - 1);
    const rightBottomElement = document.elementFromPoint(right - 1, bottom - 1);

    const items = [dropdown, dropdown.lastChild];

    return items.includes(leftBottomElement) && items.includes(rightBottomElement);
  }

  @autobind
  handleItemClick(value, title) {
    const state = { open: false };
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
    const { editable, disabled, placeholder, name, value, renderNullTitle } = this.props;
    const actualValue = this.state.selectedValue || value;
    const title = this.findTitleByValue(actualValue) || renderNullTitle(value, placeholder);

    if (editable) {
      return (
        <div className="fc-dropdown__value">
          <input name={name}
                 placeholder={placeholder}
                 disabled={disabled}
                 defaultValue={title}
                 key={`${name}-${actualValue}-selected`} />
        </div>
      );
    }

    return (
      <div className="fc-dropdown__value" onClick={this.handleToggleClick}>
        {title}
        <input name={name} type="hidden" value={actualValue} readOnly />
      </div>
    );
  }

  get dropdownButton() {
    const className = this.state.open ? 'icon-chevron-up' : 'icon-chevron-down';
    return (
      <div className="fc-dropdown__button"
           disabled={this.props.disabled}
           onClick={this.handleToggleClick}>
        <i className={className}></i>
      </div>
    );
  }

  @autobind
  onBlur() {
    this.setState({ open: false });
  }

  componentWillReceiveProps(newProps) {
    this.setState({
      selectedValue: newProps.value,
    });
  }

  render() {
    const { primary, editable, items, children, disabled, name } = this.props;
    const { open, dropup } = this.state;
    const className = classNames(this.props.className, {
      'fc-dropdown': true,
      '_primary': primary,
      '_editable': editable,
      '_open': open,
      '_disabled': disabled
    });
    const itemsClassName = classNames('fc-dropdown__items', {
      '_dropup': dropup,
      '_dropdown': !dropup,
    });

    return (
      <div className={className} onBlur={this.onBlur} tabIndex="0">
        <div className="fc-dropdown__controls" onClick={editable ? this.handleToggleClick : null}>
          {this.dropdownButton}
          {this.input}
        </div>
        <ul ref="items" className={itemsClassName}>
          {items && _.map(items, ([value, title]) => (
            <DropdownItem value={value} key={`${name}-${value}`} onSelect={this.handleItemClick}>
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
    PropTypes.bool,
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
