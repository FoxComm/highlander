/* @flow */

import classNames from 'classnames';
import React from 'react';
import type { HTMLElement } from '../types';

function wrapSpinner(svg: HTMLElement, className: string) {
  if (className.indexOf('spinner') > -1) {
    return (
      <div className="icon__spinner">{svg}</div>
    );
  }

  return svg;
}

type IconSize = 'm' | 'l' | 'xl' | 'xxl';

type IconProps = {
  size: ?IconSize;
  name: string,
  className: ?string,
  onClick: ?Function,
}

const Icon = (props: IconProps) => {
  const name = `#${props.name}-icon`;
  const size = props.size || 's';
  const useTag = `<use xlink:href=${name} />`;

  const className = classNames(
    'icon',
    `icon--${props.name}`,
    `icon--${size}`,
    props.className
  );

  const svgNode = (
    <svg className="icon__cnt" dangerouslySetInnerHTML={{__html: useTag }}/>
  );

  return (
    <div className={className} onClick={props.onClick}>
      {wrapSpinner(svgNode, className)}
    </div>
  );
};

export default Icon;
