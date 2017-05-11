/* @flow */
import React from 'react';

// webpack-svgstore-plugin will replace it with path to generated sprite
let __svg__ = { path: '../../images/icons/*.svg', name: 'symbols.svg' };
// @todo [hash]. https://github.com/mrsum/webpack-svgstore-plugin/issues/141

// Loading svg sprite to <body>
require('webpack-svgstore-plugin/src/helpers/svgxhr')({ filename: `admin/symbols.svg` });

const Icon = ({ name, className }: { name: string, className?: string }) => {
  return(
    <svg className={className}>
      <use xlinkHref={`#icon-${name}`} />
    </svg>
  );
};

export default Icon;
