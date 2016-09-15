/* @flow */

import cx from 'classnames';
import React from 'react';

import styles from './tabs.css';

import type { HTMLElement } from '../../core/types';

export type Tab = {
  key: string;
  title: string;
  active: boolean;
}

type ChangeHandler = (tab: string) => void

type Props = {
  tabs: Array<Tab>;
  onChange: ChangeHandler;
}

const renderTab = (onChange: ChangeHandler, tab: Tab) => {
  const cls = cx(styles.tab, { [styles.active]: tab.active });

  return (
    <a className={cls} onClick={() => onChange(tab.key)} key={tab.key}>{tab.title}</a>
  );
};

const Tabs = ({ tabs, onChange }: Props): HTMLElement => (
  <div className={styles.tabs}>
    {tabs.map(renderTab.bind(null, onChange))}
  </div>
);

export default Tabs;
