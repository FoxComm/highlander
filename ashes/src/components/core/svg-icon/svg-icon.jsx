/* @flow */
import React from 'react';

// webpack-svgstore-plugin will replace it with path to generated sprite
let __svg__ = { path: '../../../images/**/*.svg', name: 'symbols.svg' };
// @todo [hash]. https://github.com/mrsum/webpack-svgstore-plugin/issues/141

// Remove when mocha
if (process.env.NODE_ENV !== 'test') {
  // Loading svg sprite to <body>
  require('webpack-svgstore-plugin/src/helpers/svgxhr')(__svg__);
}

type Props = {
  /** icon type */
  name: string,
  /** svg optional className */
  className?: string
}

/**
 * SvgIcon is a simple component for representing SVG icons.
 *
 * @function SvgIcon
 */

const SvgIcon = (props: Props) => {
  return(
    <svg className={props.className}>
      <use xlinkHref={`#icon-${props.name}`} />
    </svg>
  );
};

export default SvgIcon;
