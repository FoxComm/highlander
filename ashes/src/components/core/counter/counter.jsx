/* @flow */

// libs
import React, { Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import { IncrementButton, DecrementButton } from 'components/core/button';

// styles
import s from './counter.css';

type Props = {
  /** counter's value */
  value: number,
  /** on value change action */
  onChange: (value: number) => void,
  /** min value */
  min: number,
  /** max value */
  max: number,
  /** increment/decrement step */
  step: number,
  /** additional className */
  className?: string,
  /** disables counter if true */
  disabled?: boolean,
  /** counter's id */
  counterId?: string,
};

/**
 * Counter component is a wrapper around native number input,
 * which has increment and decrement actions
 *
 * [Mockups](https://zpl.io/Z39JBU)
 *
 * @class Counter
 */

class Counter extends Component {
  props: Props;

  static defaultProps: $Shape<Props> = {
    value: 0,
    min: 0,
    max: 100,
    step: 1,
    disabled: false,
  };

  @autobind
  setValue(value: number) {
    const { min, max, onChange } = this.props;

    if (!value && value !==0 || value < min) {
      return onChange(min)
    }

    if (value > max) {
      return null;
    }

    return onChange(value);
  }

  @autobind
  handleChange({ target }: { target: HTMLInputElement }) {
    return this.setValue(parseInt(target.value, 10));
  }

  render() {
    const {
      value,
      min,
      max,
      step,
      disabled,
      counterId,
      className,
      onChange,
      ...rest,
    } = this.props;

    return (
      <div id={counterId} className={ classNames(s.counter, className)}>
        <DecrementButton
          type="button"
          disabled={disabled || value === min}
          onClick={() => this.setValue(value - step)}
          className={s.controls}
        />
        <input
          type="number"
          value={value}
          disabled={disabled}
          className={classNames(s.input, '__cssmodules')}
          onChange={this.handleChange}
          {...rest}
        />
        <IncrementButton
          type="button"
          disabled={disabled || value === max}
          onClick={() => this.setValue(value + step)}
          className={s.controls}
        />
      </div>
    );
  }
}

export default Counter;
