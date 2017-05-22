/* @flow */

// libs
import classNames from 'classnames';
import React from 'react';

// styles
import s from './checkbox.css';

type Props = {
  /** ID for input */
  id: string,
  /** Styling for DefaultCheckbox */
  className?: string,
  /** Label text */
  children?: string,
  /** Inline mode */
  inline?: boolean,
  /** Align checkbox left/right */
  docked?: string,
  /** If true - sets ("-") in checkbox  */
  halfChecked?: boolean,
  /** If true - sets ("✔") in checkbox */
  checked?: boolean,
};

/**
 * Checkbox, PartialCheckbox and SliderCheckbox - are simple components build on the top of generic DefaultCheckbox
 * and serve to show true/false data
 *
 * [Mockups](https://zpl.io/Z39JBU)
 *
 * @function Checkbox
 */

export const Checkbox = (props: Props) => {
  const { inline, docked } = props;

  const className = classNames(
    s.checkbox,
    { [s.inline]: inline },
    { [s.dockedLeft]: docked === 'left' },
    { [s.dockedRight]: docked === 'right' },
    props.className
  );

  return (
    <DefaultCheckbox {...props}
      className={ className } />
  );
};

Checkbox.defaultProps = {
  inline: false,
  docked: 'left',
};

export const PartialCheckbox = (props: Props) => {
  const className = classNames(
    { [s.halfChecked]: props.checked && props.halfChecked },
  );

  return (
    <Checkbox { ...props } className={ className }/>
  );
};

PartialCheckbox.defaultProps = {
  halfChecked: false,
  checked: false,
};

export const SliderCheckbox = (props: Props) => {
  return (
    <DefaultCheckbox {...props}
      className={ classNames(s.slideCheckbox, props.className) } />
  );
};

const DefaultCheckbox = (props: Props)=> {
  const { className, children, id, ...rest } = props;

  const labelCls = classNames(s.checkbox, {[s.label]: true });
  const label = children ? <span className={labelCls}>{children}</span> : null;

  return (
    <div className={ className }>
      <input type="checkbox" id={id} {...rest} />
      <label htmlFor={id}>
        {label}
      </label>
    </div>
  );
};
