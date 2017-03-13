// @flow

import React from 'react';
import s from './text-input.css';
import classNames from 'classnames';

type Props = {
  className?: string,
  pos?: string, // could be any combination of characters l, r, b, t or one of "middle-v", "middle-h"
  error?: boolean,
}

const TextInput = (props: Props) => {
  let positions = [];
  if (props.pos === 'middle-v') {
    positions = ['t', 'b'];
  } else if (props.pos === 'middle-h') {
    positions = ['r', 'l'];
  } else if (props.pos) {
    positions = props.pos.split('');
  }
  const posClassNames = positions.map(side => s[`pos-${side}`]);

  const {className, ...rest} = props;

  const cls = classNames(s.textInput, posClassNames, className, {
    [s.error]: props.error,
  });

  return <input className={cls} type="text" {...rest} />;
};

export default TextInput;
