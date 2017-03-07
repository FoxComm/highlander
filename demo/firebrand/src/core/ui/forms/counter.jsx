/* @flow weak */

// libs
import React, { PropTypes } from 'react';
import styles from './counter.css';

// components
import Button from '../buttons';

const Counter = props => {
  const {decreaseAction, increaseAction, ...rest} = props;

  return (
    <div styleName="container">
      <div styleName="counter-prepend">
        <Button onClick={decreaseAction} styleName="counter-button">
          -
        </Button>
      </div>
      <input
        type="number"
        styleName="counter-field"
        readOnly
        {...rest}
      />
      <div styleName="counter-append">
        <Button onClick={increaseAction} styleName="counter-button">
          +
        </Button>
      </div>
    </div>
  );
};

Counter.propTypes = {
  value: PropTypes.number,
  min: PropTypes.number,
  max: PropTypes.number,
  step: PropTypes.number,
  decreaseAction: PropTypes.func,
  increaseAction: PropTypes.func,
};

Counter.defaultProps = {
  value: 1,
  step: 1,
  min: 1,
  max: 100,
};

export default Counter;
