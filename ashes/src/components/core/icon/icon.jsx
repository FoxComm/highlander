/* @flow */
import React from 'react';

// webpack-svgstore-plugin will replace it with path to generated sprite
let __svg__ = { path: '../../../images/**/*.svg', name: 'symbols.svg' };
// @todo [hash]. https://github.com/mrsum/webpack-svgstore-plugin/issues/141

// Remove when mocha
if (process.env.NODE_ENV !== 'test') {
  // Loading svg sprite to <body>
  require('webpack-svgstore-plugin/src/helpers/svgxhr')({ filename: `admin/symbols.svg` });
}

type Props = {
  /** icon type */
  name: string,
  /** svg optional className */
  className?: string
}

/**
 * Icon is a simple component for representing SVG icons.
 *
 * @function Icon
 */

const Icon = (props: Props) => {
  return(
    <svg className={props.className}>
      <use xlinkHref={`#icon-${props.name}`} />
    </svg>
  );
};

export default Icon;
