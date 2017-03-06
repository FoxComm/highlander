
import React from 'react';
import styles from './css/term-value-line.css';

import type { HTMLElement } from 'types';

type PropsType = {
  children: Array<HTMLElement|string>;
  className?: string;
}

const TermValueLine = (props: PropsType) => {
  return (
    <div styleName="term-value-line" {...props}>
      <div styleName="term">{props.children[0]}</div>
      <div styleName="value">{props.children[1]}</div>
    </div>
  );
};

export default TermValueLine;
