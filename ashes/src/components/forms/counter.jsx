/* @flow */

// libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// components
import { IncrementButton, DecrementButton } from '../common/buttons';

type Props = {
  value?: number,
  min?: number,
  max?: number,
  step?: number,
  decreaseAction: Function,
  increaseAction: Function,
  className?: string,
  disabled?: boolean,
  counterId?: string,
};

const Counter = ({
  value = 1,
  min = 1,
  max = 100,
  step = 1,
  disabled = false,
  decreaseAction,
  increaseAction,
  className,
  counterId,
  ...rest,
}: Props) => {
  return (
    <div id={counterId} className="fc-input-group fc-counter">
      <div className="fc-input-prepend">
        <DecrementButton
          type="button"
          disabled={disabled || value === min}
          onClick={decreaseAction}
          className={ classNames('fc-btn-counter', 'decrement-btn') }
        />
      </div>
      <input
        type="number"
        value={value}
        className={classNames('fc-counter__input', className, 'adjust-quantity-input', {disabled})}
        disabled={disabled}
        {...rest} />
      <div className="fc-input-append">
        <IncrementButton
          type="button"
          disabled={disabled || value === max}
          onClick={increaseAction}
          className={ classNames('fc-btn-counter', 'increment-btn') }
        />
      </div>
    </div>
  );
};

export default Counter;
