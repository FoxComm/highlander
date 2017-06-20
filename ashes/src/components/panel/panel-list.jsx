/* @flow */

// libs
import classNames from 'classnames';
import React, { Element } from 'react';

// styles
import s from './panel-list.css';

type PanelListProps = {
  className?: string;
  children?: Element<*>;
};

export const PanelList = ({ className, children }: PanelListProps) => (
  <div className={classNames(s.block, className)}>
    {children}
  </div>
);

type PanelListItemProps = {
  title: Element<*>|string;
  children?: Element<*>;
};

export const PanelListItem = ({ title, children }: PanelListItemProps) => (
  <div className={s.item}>
    <header className={s.header}>
      {title}
    </header>
    <div className={s.content}>
      {children}
    </div>
  </div>
);
