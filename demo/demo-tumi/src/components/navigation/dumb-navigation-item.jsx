/* @flow */

import React, { Element } from 'react';
import { Link } from 'react-router';
import classNames from 'classnames';

import styles from './navigation.css';

type Props = {
  isActive?: boolean,
  isHighlighted?: boolean,
  extraContent?: Element<*>|string,
  className?: string,
  to?: string|{name: string, params: Object},
  onClick?: (event: SyntheticEvent) => void,
  blockProps?: Object,
  linkProps?: Object,
  linkContent: Element<*>|string
}

const DumbNavigationItem = (props: Props) => {
  const linkBlockClasses = classNames(styles.item, props.className, {
    [styles.active]: props.isActive,
    [styles['is-highlighted']]: props.isHighlighted,
  });

  const linkClass = classNames(styles['item-link'], {
    [styles['is-highlighted']]: props.isHighlighted,
  });

  const { blockProps = {}, linkProps = {} } = props;

  const finalLinkProps = {
    className: linkClass,
    onClick: props.onClick,
    ...linkProps,
  };
  if (props.to) finalLinkProps.to = props.to;

  const body = props.to ?
    <Link {...finalLinkProps}>{props.linkContent}</Link> :
    <button {...finalLinkProps}>{props.linkContent}</button>;


  return (
    <div
      className={linkBlockClasses}
      {...blockProps}
    >
      { body }
      { props.extraContent }
    </div>
  );
};

export default DumbNavigationItem;
