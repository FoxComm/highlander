
/* @flow */

import React, { Element } from 'react';

import ActionLink from 'ui/action-link/action-link';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import styles from './editable-block.css';

type EditableProps = Localized & {
  isEditing: boolean,
  collapsed?: boolean,
  editAllowed?: boolean,
  className?: string,
  content?: ?Element<*>,
  children?: Element<*>,
  editAction?: () => any,
  actionsContent?: Element<*>|Array<Element<*>>,
  title: string|Element<*>,
  t: any,
};

const EditableBlock = (props: EditableProps) => {
  const editLink = () => {
    if (!props.isEditing && !props.collapsed && props.editAllowed) {
      return (
        <ActionLink
          action={props.editAction}
          title={props.t('Edit')}
          styleName="action"
        />
      );
    }
  };

  const actions = props.actionsContent || editLink();
  const content = !props.collapsed ? (props.content || props.children) : null;

  return (
    <article styleName="editable-block" className={props.className}>
      <header styleName="header">
        <h3 styleName="title">{props.title}</h3>
        {actions}
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
