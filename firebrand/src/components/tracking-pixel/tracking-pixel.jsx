/* @flow */

// libs
import React from 'react';
import type { HTMLElement } from 'types';

// styles
import styles from './tracking-pixel.css';

type Props = {
  url: string;
  channel: number;
  subject: number;
  obj: string;
  verb: string;
  objId: number;
}

const TrackingPixel = (props: Props): HTMLElement => {
  const salt = Math.floor(Math.random()*100000)
  const fullUrl = `${props.url}?ch=${props.channel}&sub=${props.subject}&v=${props.verb}&ob=${props.obj}&id=${props.objId}&salt=${salt}`;

  return (
    <img styleName="tracking-style" alt="" src={fullUrl} />
  );
};

export default TrackingPixel;
