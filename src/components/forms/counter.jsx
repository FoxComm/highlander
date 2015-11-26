import React, { PropTypes } from 'react';
import { IncrementButton, DecrementButton } from '../common/buttons';

const Counter = props => {
  return (
    <div className="fc-input-group fc-counter">
      <div className="fc-input-prepend">
        <DecrementButton onClick={props.decreaseAction} className="fc-btn-counter" />
      </div>
      <input
        type="number"
        id={props.inputName}
        name={props.inputName}
        value={props.value}
        min={props.minValue}
        max={props.maxValue}
        step={props.stepAmount}
        onChange={props.onChange} />
      <div className="fc-input-append">
        <IncrementButton onClick={props.increaseAction} className="fc-btn-counter" />
      </div>
    </div>
  );
};

Counter.propTypes = {
  inputName: PropTypes.string,
  value: PropTypes.number,
  minValue: PropTypes.number,
  maxValue: PropTypes.number,
  stepAmount: PropTypes.number,
  decreaseAction: PropTypes.func,
  increaseAction: PropTypes.func,
};

Counter.defaultProps = {
  value: 1,
  stepAmount: 1,
  minValue: 1,
  maxValue: 100
};

export default Counter;
