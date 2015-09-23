'use strict';

import React from 'react';

export default class Counter extends React.Component {
  constructor(props) {
    super(props);

    let value = null;
    if (this.props.model) {
      value = +this.props.model[props.defaultValue];
    } else {
      value = +this.props.defaultValue;
    }

    this.state = {
      value: value
    };
  }

  triggerChange() {
    if (this.props.onChange) {
      this.props.onChange(this.state.value);
    }
  }

  decreaseTotal(event) {
    event.preventDefault();
    document.getElementById(this.props.inputName).stepDown(this.props.stepAmount);
    this.setState({
      value: this.state.value - 1
    }, () => {
      this.triggerChange();
    });
  }

  increaseTotal(event) {
    event.preventDefault();
    document.getElementById(this.props.inputName).stepUp(this.props.stepAmount);
    this.setState({
      value: this.state.value + 1
    }, () => {
      this.triggerChange();
    });
  }

  onChange(event) {
    this.setState({
      value: event.target.value
    }, () => {
      this.triggerChange();
    });
  }

  render() {
    let value = null;
    if (this.props.model) {
      value = this.props.model[this.props.defaultValue];
    } else {
      value = this.state.value;
    }

    return (
      <div className="fc-input-group fc-counter">
        <div className="fc-input-prepend">
          <button onClick={this.decreaseTotal.bind(this)}><i className="icon-chevron-down"></i></button>
        </div>
        <input
          type="number"
          id={this.props.inputName}
          name={this.props.inputName}
          value={value}
          min={this.props.minValue}
          max={this.props.maxValue}
          step={this.props.stepAmount}
          onChange={this.onChange.bind(this)} />
        <div className="fc-input-append">
          <button onClick={this.increaseTotal.bind(this)}><i className="icon-chevron-up"></i></button>
        </div>
      </div>
    );
  }
}

Counter.propTypes = {
  inputName: React.PropTypes.string,
  defaultValue: React.PropTypes.node,
  stepAmount: React.PropTypes.number,
  minValue: React.PropTypes.number,
  maxValue: React.PropTypes.number,
  onChange: React.PropTypes.func,
  model: React.PropTypes.object
};

Counter.defaultProps = {
  defaultValue: 1,
  stepAmount: 1,
  minValue: 1,
  maxValue: 100,
  inputName: 'counter'
};
