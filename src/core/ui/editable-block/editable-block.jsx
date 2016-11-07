
/* @flow */

import React from 'react';
import styles from './editable-block.css';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import type { HTMLElement } from 'types';

type EditableProps = Localized & {
  isEditing: boolean,
  collapsed?: boolean,
  editAllowed?: boolean,
  className?: string,
  content?: HTMLElement,
  children?: HTMLElement,
  editAction?: () => any,
  title: string,
};

const EditableBlock = (props: EditableProps) => {
  const editLink = !props.isEditing && !props.collapsed && props.editAllowed
    ? <div onClick={props.editAction} styleName="action">{props.t('EDIT')}</div>
    : null;

  const content = !props.collapsed ? (props.content || props.children) : null;

  return (
    <article styleName="editable-block" className={props.className}>
      <header styleName="header">
        <h3 styleName="title">{props.title}</h3>
        {editLink}
      </header>
      {content}
    </article>
  );
};

EditableBlock.defaultProps = {
  isEditing: false,
  collapsed: false,
  editAllowed: true,
};

export default localized(EditableBlock);
