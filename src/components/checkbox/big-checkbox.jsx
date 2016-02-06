import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import _ from 'lodash';

import { Checkbox } from './checkbox';

export default class BigCheckbox extends Component {
  static propTypes = {
    label: PropTypes.string,
    name: PropTypes.string.isRequired,
    onToggle: PropTypes.func,
    value: PropTypes.bool,
  };

  static defaultProps = {
    label: '',
    onToggle: _.noop,
    value: false,
  };

  constructor(props, ...args) {
    super(props, ...args);

    this.state = {
      value: props.value,
    };
  }

  get checkbox() {
    const className = classNames('fc-big-checkbox__visible-box', {
      '_checked': this.state.value
    });
    return <div className={className} onClick={this.toggle} />;
  }

  @autobind
  toggle() {
    this.props.onToggle(!this.state.value);
    this.setState({ value: !this.state.value });
  }

  render() {
    return (
      <div className="fc-big-checkbox">
        {this.checkbox}
        <Checkbox 
          className="fc-big-checkbox__hidden-input"
          name={this.props.name}
          value={this.state.value} />
      </div>
    );
  }
}  
