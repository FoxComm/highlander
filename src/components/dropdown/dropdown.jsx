import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import DropdownItem from './dropdownItem';

export default class Dropdown extends React.Component {
  static propTypes = {
    name: PropTypes.string,
    value: PropTypes.string,
    editable: PropTypes.bool,
    primary: PropTypes.bool,
    open: PropTypes.bool,
    placeholder: PropTypes.string,
    onChange: PropTypes.func,
    items: PropTypes.object,
    children: PropTypes.node
  };

  constructor(...args) {
    super(...args);
    this.state = {
      open: !!this.props.open,
      selectedValue: '',
      selectedTitle: ''
    };
  }

  findTitleByValue(value) {
    if (this.props.items) {
      return this.props.items[value];
    } else {
      const item = _.findWhere(React.Children.toArray(this.props.children), {props: {value: value}});
      return item && item.props.children;
    }
  }

  handleToggleClick(event) {
    event.preventDefault();
    this.setState({
      open: !this.state.open
    });
  }

  handleItemClick(value, title, event) {
    event.preventDefault();
    this.setState({
      open: false,
      selectedValue: value,
      selectedTitle: title
    }, () => {
      if (this.props.onChange) {
        this.props.onChange(value, title);
      }
    });
  }

  get dropdownButton() {
    return (
      <div className="fc-dropdown-button" onClick={this.handleToggleClick.bind(this)}>
        <i className="icon-chevron-down"></i>
      </div>
    );
  }

  render() {
    const classnames = classNames({
      'fc-dropdown': true,
      'is_dropdown_primary': this.props.primary,
      'is_dropdown_editable': this.props.editable,
      'is_dropdown_open': this.state.open
    });
    const value = this.state.selectedValue || this.props.value;
    const title = this.state.selectedTitle || this.findTitleByValue(value);

    return (
      <div className={classnames}>
        {this.props.editable && (
          <div className="fc-dropdown-controls">
            {this.dropdownButton}
            <div className="fc-dropdown-value">
              <input placeholder={this.props.placeholder} defaultValue={title} key={value}/>
            </div>
          </div>
        ) || (
          <div className="fc-dropdown-controls" onClick={this.handleToggleClick.bind(this)}>
            {this.dropdownButton}
            <div className="fc-dropdown-value">
              {title || this.props.placeholder}
              <input name={this.props.name} type="hidden" value={value}/>
            </div>
          </div>
        )}
        <div className="fc-dropdown-items">
          {this.props.items && _.map(this.props.items, (title, value) => (
            <DropdownItem value={value} key={value} onClick={this.handleItemClick.bind(this, value, title)}>
              {title}
            </DropdownItem>
          )) || React.Children.map(this.props.children, item => (
              React.cloneElement(item, {
                onClick: this.handleItemClick.bind(this, item.props.value, item.props.children)
              })
            )
          )}
        </div>
      </div>
    );
  }
}
