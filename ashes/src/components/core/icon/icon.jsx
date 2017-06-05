/* @flow */
import React from 'react';
import classNames from 'classnames';

type Props = {
  /** icon type */
  name: string,
  /** additional className */
  className?: string
}

/**
 * Icon is a simple component for representing icons.
 *
 * @function Icon
 */

const Icon = (props: Props) => {
  const { name, className, ...rest } = props;
  const iconCls = classNames(`icon-${name}`, className );
  return(
    <i className={iconCls} {...rest}/>
  );
};

export default Icon;
