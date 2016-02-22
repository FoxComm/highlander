/* @flow */

import classNames from 'classnames';
import React, { PropTypes, Element } from 'react';

function wrapSpinner(svg: Element, className: string) {
  if (className.indexOf('spinner') > -1) {
    return (
      <div className="icon__spinner">{svg}</div>
    );
  }

  return svg;
}

const Icon = (props: Object) => {
  const name = `#${props.name}-icon`;
  const useTag = `<use xlink:href=${name} />`;

  const className = classNames(
    'icon',
    `icon--${props.name}`,
    props.size && `icon--${props.size}`,
    props.className
  );

  const svgNode = (
    <svg className="icon__cnt" dangerouslySetInnerHTML={{__html: useTag }}>
    </svg>
  );

  return (
    <div className={className}>
      {wrapSpinner(svgNode, className)}
    </div>
  );
};

Icon.propTypes = {
  size: PropTypes.oneOf(['m', 'l', 'xl', 'xxl']),
  name: PropTypes.string.isRequired,
  className: PropTypes.string,
};

export default Icon;
