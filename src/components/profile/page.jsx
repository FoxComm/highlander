// @flow
import React from 'react';
import type { HTMLElement } from 'types';

import styles from './page.css';

type Props = {
  children: HTMLElement,
}

const Page = (props: Props) => {
  return (
    <div>
      <h1 styleName="title">My Account</h1>
      <div styleName="content">
        {props.children}
      </div>
    </div>
  );
};

export default Page;
