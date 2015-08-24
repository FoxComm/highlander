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
    document.getElementById(this.props.inputName).stepDown(this.props.stepAmount);
  }

  increaseTotal(event) {
    event.preventDefault();
    document.getElementById(this.props.inputName).stepUp(this.props.stepAmount);
  }

  onChange(event) {
    this.setState({
      inputValue: event.target.value
    });
  }

  render() {
    return (
      <div className="fc-input-group">
        <div className="fc-input-prepend">
          <button onClick={this.decreaseTotal.bind(this)}><i className="icon-down-dir"></i></button>
        </div>
        <input type="number" id={this.props.inputName} name={this.props.inputName} value={this.state.inputValue} onChange={this.onChange.bind(this)} min={this.props.minValue} max={this.props.maxValue} step={this.props.stepAmount} />
        <div className="fc-input-append">
          <button onClick={this.increaseTotal.bind(this)}><i className="icon-up-dir"></i></button>
        </div>
      </div>
    );
  }
}

Counter.propTypes = {
  inputName: React.PropTypes.string,
  defaultValue: React.PropTypes.number,
  stepAmount: React.PropTypes.number,
  minValue: React.PropTypes.number,
  maxValue: React.PropTypes.number
};

Counter.defaultProps = {
  defaultValue: 1,
  stepAmount: 1,
  minValue: 1,
  maxValue: 100
};
