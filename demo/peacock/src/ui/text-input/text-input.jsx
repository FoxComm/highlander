// @flow

import React from 'react';
import s from './text-input.css';
import classNames from 'classnames';

type Props = {
  className?: string,
  pos?: string, // could be any combination of characters l, r, b, t or "middle"
}

const TextInput = (props: Props) => {
  const positions = props.pos == 'middle' ? ['t', 'b'] : (props.pos || '').split('');
  const posClassNames = positions.map(side => s[`pos-${side}`]);

  const {className, ...rest} = props;

  const cls = classNames(s.textInput, posClassNames, className);

  return <input className={cls} type="text" {...rest} />;
};

export default TextInput;
