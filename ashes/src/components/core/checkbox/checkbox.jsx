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
  /** If checkbox is half checked. Sign: ("-")  */
  halfChecked?: boolean,
  /** If checkbox is checked Sign ("✔") */
  checked?: boolean,
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

const SliderCheckbox = (props: Props) => {
  return (
    <DefaultCheckbox {...props}
      className={ classNames(s.slideCheckbox, props.className) } />
  );
};

const Checkbox = (props: Props) => {
  const { inline, docked, halfChecked } = props;
  const className = classNames(
    s.checkbox,
    {
      [s.halfChecked]: halfChecked,
      [s.inline]: inline,
      [s.dockedLeft]: docked === 'left',
      [s.dockedRight]: docked === 'right'
    }
  );

  return (
    <DefaultCheckbox {...props}
      className={ className } />
  );
};

Checkbox.defaultProps = {
  inline: false,
  docked: 'left'
};

const HalfCheckbox = (props: Props)=> {
  return (
    <Checkbox {...props} halfChecked={props.checked && props.halfChecked} />
  );
};

HalfCheckbox.defaultProps = {
  halfChecked: false,
  checked: false,
};


export {
  SliderCheckbox,
  Checkbox,
  HalfCheckbox,
};
