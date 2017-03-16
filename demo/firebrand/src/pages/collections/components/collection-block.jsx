/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { Link } from 'react-router';

import styles from './collection-block.css';

type LinkInfo = {
  to?: string;
  onClick?: Function;
  text: string;
}

type Props = {
  header: string;
  description: string;
  link: LinkInfo;
}

const CollectionBanner = (props: Props): HTMLElement => (
  <article styleName="banner">
    <h2 styleName="header">{props.header}</h2>
    <h3 styleName="description">{props.description}</h3>
    <Link
      key={`link-${props.link.to}`}
      styleName="link"
      to={props.link.to ? props.link.to : null}
      onClick={props.link.onClick ? props.link.onClick : null}
    >
      {props.link.text}
    </Link>
  </article>
);

export default CollectionBanner;
