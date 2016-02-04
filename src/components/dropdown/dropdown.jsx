import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import DropdownItem from './dropdownItem';

export default class Dropdown extends React.Component {

  static itemsType = PropTypes.arrayOf(PropTypes.array);

  static propTypes = {
    name: PropTypes.string,
    className: PropTypes.string,
    value: PropTypes.string,
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

  static defaultProps = {
    renderNullTitle: (value, placeholder) => {
      return placeholder;
    },
    changeable: true,
  };

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
    const {editable, placeholder, name, value, renderNullTitle} = this.props;
    const actualValue = this.state.selectedValue || value;
    const title = this.findTitleByValue(actualValue) || renderNullTitle(value, placeholder);

    if (editable) {
      return (
        <div className="fc-dropdown__value">
          <input placeholder={placeholder} defaultValue={title} key={actualValue} />
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
      <div className="fc-dropdown__button" onClick={this.handleToggleClick}>
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
    const classnames = classNames(this.props.className, {
      'fc-dropdown': true,
      '_primary': this.props.primary,
      '_editable': this.props.editable,
      '_open': this.state.open
    });

    return (
      <div className={classnames} onBlur={this.onBlur} tabIndex="0">
        <div className="fc-dropdown__controls" onClick={this.props.editable ? this.handleToggleClick : null}>
          {this.dropdownButton}
          {this.input}
        </div>
        <ul className="fc-dropdown__items">
          {this.props.items && _.map(this.props.items, ([value, title]) => (
            <DropdownItem value={value} key={value} onSelect={this.handleItemClick}>
              {title}
            </DropdownItem>
          )) || React.Children.map(this.props.children, item => (
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
