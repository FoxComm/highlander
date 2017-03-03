
/* @flow */

import React from 'react';
import styles from './editable-block.css';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import type { HTMLElement } from 'types';

type EditableProps = Localized & {
  isEditing: boolean;
  collapsed?: boolean;
  editAllowed?: boolean;
  className?: string;
  content: HTMLElement;
  editAction?: () => any;
  title: string;
};

const EditableBlock = (props: EditableProps) => {
  const editLink = !props.isEditing && !props.collapsed && props.editAllowed
    ? <div onClick={props.editAction} styleName="edit">{props.t('EDIT')}</div>
    : null;

  const content = !props.collapsed ? props.content : null;

  return (
    <div styleName="editable-block" {...props}>
      <div styleName="header">
        <div styleName="title">{props.title}</div>
        {editLink}
      </div>
      {content}
    </div>
  );
};

EditableBlock.defaultProps = {
  isEditing: false,
  collapsed: false,
  editAllowed: true,
};

export default localized(EditableBlock);
