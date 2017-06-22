/* @flow */

import React, { Element } from 'react';

import { Checkbox } from 'components/core/checkbox';

import styles from './selectable-list.css';

type Props = {
  id: number,
  onToggle?: (id: number) => void,
  checked?: boolean,
  title?: string | Element<*>,
  children?: string | Element<*>,
};

const SelectableItem = ({ id, onToggle = id => {}, checked = false, title, children }: Props) => {
  const handleItemClick = (event: SyntheticEvent) => {
    event.stopPropagation();
    event.preventDefault();

    onToggle(id);
  };

  const listId = `selectable-list-${id}`;
  const content = children || <strong>{title}</strong>;

  return (
    <li styleName="item" key={id} onClick={handleItemClick}>
      <Checkbox
        id={listId}
        label={content}
        checked={checked}
        onChange={() => onToggle(id)}
        onClick={(event: SyntheticEvent) => event.stopPropagation()}
      />
    </li>
  );
};

export default SelectableItem;
