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
      value: value,
      previousValue: value
    };
  }

  triggerChange() {
    if (this.props.onChange) {
      this.props.onChange(this.state.previousValue, this.state.value);
    }
  }

  setValues(oldValue, newValue) {
    this.setState({
      value: newValue,
      previousValue: oldValue
    }, () => {
      this.triggerChange();
    });
  }

  performStep(method, modifier) {
    let input = document.getElementById(this.props.inputName);
    input[method](this.props.stepAmount);
    let newValue = this.state.value + (this.props.stepAmount * modifier);
    let oldValue = this.state.value;
    if (this.props.onBeforeChange) {
      this.props.onBeforeChange(oldValue, newValue, (success) => {
        if (success) {
          this.setValues(oldValue, newValue);
        }
      });
    } else {
      this.setValues(oldValue, newValue);
    }
  }

  decreaseTotal(event) {
    event.preventDefault();
    this.performStep('stepDown', -1);
  }

  increaseTotal(event) {
    event.preventDefault();
    this.performStep('stepUp', 1);
  }

  onFocus(event) {
    this.setState({
      previousValue: event.target.value
    });
  }

  onChange(event) {
    let target = event.target;
    let oldValue = this.state.previousValue;
    let newValue = target.value;
    if (this.props.onBeforeChange) {
      this.props.onBeforeChange(oldValue, newValue, (success) => {
        if (success) {
          this.setValues(oldValue, newValue);
        } else {
          target.value = oldValue;
        }
      });
    }
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
          onChange={this.onChange.bind(this)}
          onFocus={this.onFocus.bind(this)} />
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
  onBeforeChange: React.PropTypes.func,
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
