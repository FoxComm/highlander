/* @flow */

import React from 'react';
import type { HTMLElement } from 'types';
import Banner from '../banner/banner';

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
  styleName?: string;
}

const BannerWithImage = (props: Props): HTMLElement => {
  const {styleName, ...other} = props;

  return (
    <div styleName={styleName}>
      <Banner {...other}/>
      <div styleName={`${styleName}__image`}></div>
    </div>
  );
};

export default BannerWithImage;
