// @flow
import React from 'react';
import type { HTMLElement } from 'types';

import styles from './block.css';

type BlockProps = {
  title: string,
  children: HTMLElement|Array<HTMLElement>,
}

const Block = (props: BlockProps) => {
  return (
    <div styleName="block">
      <div styleName="header">{props.title}</div>
      <div styleName="content">{props.children}</div>
    </div>
  );
};

export default Block;
