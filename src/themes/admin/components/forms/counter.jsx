'use strict';

import React from 'react';

export default class Counter extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      inputValue: +props.defaultValue
    };
  }

  decreaseTotal(event) {
    event.preventDefault();
    let newValue = this.state.inputValue - this.props.stepAmount;

    this.setState({
      inputValue: newValue < this.props.minValue ? this.props.minValue : newValue
    });
  }

  increaseTotal(event) {
    event.preventDefault();
    this.setState({
      inputValue: this.state.inputValue + this.props.stepAmount
    });
  }

  onChange(event) {
    this.setState({
      inputValue: event.target.value
    });
  }

  render() {
    return (
      <div>
        <button onClick={this.decreaseTotal.bind(this)}><i className="icon-down-dir"></i></button>
        <input type="number" name={this.props.inputName} value={this.state.inputValue} onChange={this.onChange.bind(this)} className="control" />
        <button onClick={this.increaseTotal.bind(this)}><i className="icon-up-dir"></i></button>
      </div>
    );
  }
}

Counter.propTypes = {
  inputName: React.PropTypes.string,
  defaultValue: React.PropTypes.number,
  stepAmount: React.PropTypes.number,
  minValue: React.PropTypes.number
};

Counter.defaultProps = {
  defaultValue: 1,
  stepAmount: 1,
  minValue: 1
};
