'use strict';

import React from 'react';
import ClassNames from 'classnames';

export default class Dropdown extends React.Component {
  static propTypes = {
    value: React.PropTypes.string,
    editable: React.PropTypes.bool,
    primary: React.PropTypes.bool,
    open: React.PropTypes.bool,
    placeholder: React.PropTypes.string,
    children: React.PropTypes.any,
    onChange: React.PropTypes.func
  };

  constructor(...args) {
    super(...args);
    this.state = {
      open: !!this.props.open,
      selectedItem: {
        props: {
          value: this.props.value,
          children: ''
        }
      }
    };
  }

  findItemByValue(value) {
    return React.Children.toArray(this.props.children).filter(item => item.props.value === value)[0];
  }

  handleToggleClick(event) {
    event.preventDefault();
    this.setState({
      open: !this.state.open
    });
  }

  handleItemClick(item, event) {
    event.preventDefault();
    this.setState({
      open: false,
      selectedItem: item
    }, () => {
      if (this.props.onChange) {
        this.props.onChange(item.key);
      }
    });
  }

  render() {
    const classnames = ClassNames({
      'fc-dropdown': true,
      'is_primary': this.props.primary,
      'is_editable': this.props.editable,
      'is_open': this.state.open
    });
    const selectedItem = this.state.selectedItem;
    const key = selectedItem && selectedItem.props.value;

    let value = selectedItem && selectedItem.props.children;
    if (!value) {
      let itemByValue = this.props.value && this.findItemByValue(this.props.value);
      value = itemByValue && itemByValue.props.children;
    }

    const button = (
      <div className="fc-dropdown-button" onClick={this.handleToggleClick.bind(this)}>
        <i className="icon-chevron-down"></i>
      </div>
    );

    return (
      <div className={classnames}>
        {this.props.editable && (
          <div className="fc-dropdown-controls">
            {button}
            <div className="fc-dropdown-value">
              <input placeholder={this.props.placeholder} defaultValue={value} key={key}/>
            </div>
          </div>
        ) || (
          <div className="fc-dropdown-controls" onClick={this.handleToggleClick.bind(this)}>
            {button}
            <div className="fc-dropdown-value">
              {value || this.props.placeholder}
            </div>
          </div>
        )}
        <div className="fc-dropdown-items">
          {React.Children.map(this.props.children, (item, index) => {
            return React.cloneElement(item, {
              onClick: this.handleItemClick.bind(this, item)
            });
          })}
        </div>
      </div>
    );
  }
}
