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
    primary: PropTypes.bool,
    open: PropTypes.bool,
    placeholder: PropTypes.string,
    onChange: PropTypes.func,
    items: Dropdown.itemsType,
    children: PropTypes.node,
  };

  constructor(...args) {
    super(...args);
    this.state = {
      open: !!this.props.open,
      selectedValue: '',
    };
  }

  findTitleByValue(value, props) {
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
  handleItemClick(value, title, event) {
    event.preventDefault();
    this.setState({
      open: false,
      selectedValue: value
    }, () => {
      if (this.props.onChange) {
        this.props.onChange(value, title);
      }
    });
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
    const value = this.state.selectedValue || this.props.value;
    const title = this.findTitleByValue(value, this.props);

    return (
      <div className={classnames} onBlur={this.onBlur} tabIndex="0">
        {this.props.editable && (
          <div className="fc-dropdown__controls">
            {this.dropdownButton}
            <div className="fc-dropdown__value">
              <input placeholder={this.props.placeholder} defaultValue={title} key={value}/>
            </div>
          </div>
        ) || (
          <div className="fc-dropdown__controls" onClick={this.handleToggleClick}>
            {this.dropdownButton}
            <div className="fc-dropdown__value">
              {title || this.props.placeholder}
              <input name={this.props.name} type="hidden" value={value}/>
            </div>
          </div>
        )}
        <ul className="fc-dropdown__items">
          {this.props.items && _.map(this.props.items, ([value, title]) => (
            <DropdownItem value={value} key={value} onClick={(event) => this.handleItemClick(value, title, event)}>
              {title}
            </DropdownItem>
          )) || React.Children.map(this.props.children, item => (
              React.cloneElement(item, {
                onClick: (event) => this.handleItemClick(item.props.value, item.props.children, event)
              })
            )
          )}
        </ul>
      </div>
    );
  }
}
