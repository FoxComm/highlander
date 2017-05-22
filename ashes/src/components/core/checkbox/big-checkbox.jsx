/* @flow */

// libs
import React, { Component } from 'react';
import classNames from 'classnames';
import { noop, get } from 'lodash';

// components
import { Checkbox } from 'components/core/checkbox';

// styles
import s from './big-checkbox.css';

type Props = {
  /** ID for checkbox input */
  id: string,
  /** name for checkbox input */
  name?: string,
  /** action performed on toggle*/
  onToggle?: Function,
  /** if true - sets checkbox to 'checked' */
  value?: boolean,
};

/**
 * BigCheckbox is the component built on top of Checkbox.
 *
 * @function BigCheckbox
 */

export const BigCheckbox = (props: Props) => {
  const onToggle = get(props, 'onToggle');
  const checkboxClass = classNames(
    s.bigCheckbox,
    s.visibleBox,
    { [s.checked]: props.value }
  );

  return (
    <div className={s.bigCheckbox}>
      <div className={checkboxClass} onClick={() => onToggle(!props.value)} />
      <Checkbox
        className={ classNames(s.bigCheckbox, s.hiddenInput) }
        id={props.id}
        name={props.name}
        value={props.value}
      />
    </div>
  );
};

BigCheckbox.defaultProps = {
  onToggle: noop,
  value: false,
};
