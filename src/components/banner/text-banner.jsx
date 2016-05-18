/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';

import styles from './text-banner.css';

type Props = {
  header: string;
  children: Object|HTMLElement|string;
}

const TextBanner = (props: Props): HTMLElement => (
  <article styleName="banner">
    <h2 styleName="header">{props.header}</h2>
    <span styleName="description">{props.children}</span>
  </article>
);

export default TextBanner;
