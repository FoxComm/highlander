/* @flow */
import React from 'react';

// webpack-svgstore-plugin will replace it with path to generated sprite
let __svg__ = { path: '../../../images/**/*.svg', name: 'symbols.svg' };
// @todo [hash]. https://github.com/mrsum/webpack-svgstore-plugin/issues/141

// Remove when mocha
// @todo why we do that? Works fine without condition
if (process.env.NODE_ENV !== 'test') {
  // Loading svg sprite to <body>
  require('webpack-svgstore-plugin/src/helpers/svgxhr')(__svg__);
}

type Props = {
  /** icon type */
  name: string,
  /** svg optional className */
  className?: string,
};

/**
 * SvgIcon is a simple component for representing SVG icons.
 *
 * @function SvgIcon
 */

const SvgIcon = (props: Props) => {
  const { name, className, ...rest } = props;
  return (
    <svg className={className} {...rest}>
      <use xlinkHref={`#icon-${name}`} />
    </svg>
  );
};

export default SvgIcon;
