/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import { Link } from 'react-router';

import styles from './banner.css';

type LinkInfo = {
  to?: string;
  onClick?: Function;
  text: string;
}

type Props = {
  header: string;
  description: string;
  links: Array<LinkInfo>;
}

const Banner = (props: Props): HTMLElement => (
  <article styleName="banner">
    <h2 styleName="header">{props.header}</h2>
    <h3 styleName="description">{props.description}</h3>
    <div styleName="links">
      {props.links.map((link: LinkInfo, index) => (
        <Link
          key={`link-${link.to}-${index}`}
          styleName="link"
          to={link.to ? link.to : null}
          onClick={link.onClick ? link.onClick : null}
        >
          {link.text}
        </Link>
      ))}
    </div>
  </article>
);

export default Banner;
