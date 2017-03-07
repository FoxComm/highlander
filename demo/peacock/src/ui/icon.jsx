/* @flow */

import classNames from 'classnames';
import React from 'react';

function wrapSpinner(svg: string, className: string) {
  if (className.indexOf('spinner') > -1) {
    return (
      `<span class="icon__spinner">${svg}</span>`
    );
  }

  return svg;
}

type IconSize = 'custom' | 'm' | 'l' | 'xl' | 'xxl';

type IconProps = {
  size: ?IconSize,
  name: string,
  className: ?string,
  onClick: ?Function,
};

const Icon = (props: IconProps) => {
  const name = `#${props.name}-icon`;
  const size = props.size || 's';

  const className = classNames(
    'icon',
    `icon--${props.name}`,
    `icon--${size}`,
    props.className
  );

  const svg =
    `<svg class="icon__cnt">
      <!-- SAFARI TAB NAVIGATION FIX -->
      <use xlink:href=${name} />
      <!-- SAFARI TAB NAVIGATION FIX -->
    </svg>`;

  const svgNode = wrapSpinner(svg, className);

  return (
    <span
      className={className}
      onClick={props.onClick}
      dangerouslySetInnerHTML={{__html: svgNode }}
    />
  );
};

export default Icon;
