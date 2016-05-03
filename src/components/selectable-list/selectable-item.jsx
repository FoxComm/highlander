/* @flow */

import React, { Element } from 'react';

import { Checkbox } from '../checkbox/checkbox';

import styles from './selectable-list.css';

type Props = {
  id: number;
  onSelect: (id: number) => void;
  checked: boolean;
  title?: string|Element;
  children?: string|Element;
}

const SelectableItem = (props: Props) => {
  const handleItemClick = (event: SyntheticEvent) => {
    event.stopPropagation();
    event.preventDefault();

    props.onSelect(props.id);
  };

  const handleCheckboxClick = (event: SyntheticEvent) => {
    event.stopPropagation();

    props.onSelect(props.id);
  };

  const id = `selectable-list-${props.id}`;
  const content = props.children || <strong>{props.title}</strong>;

  return (
    <li styleName="item" key={props.id} onClick={handleItemClick}>
      <Checkbox id={id} checked={props.checked} onClick={handleCheckboxClick}>
        {content}
      </Checkbox>
    </li>
  );
};

export default SelectableItem;
