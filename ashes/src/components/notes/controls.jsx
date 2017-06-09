/* @flow */

// libs
import React from 'react';

// components
import DetailedInitials from '../user-initials/detailed-initials';
import { EditButton, DeleteButton } from 'components/core/button';

// styles
import s from './notes.css';

type Props = {
  model: {
    author: Object,
  },
  onEditClick: Function,
  onDeleteClick: Function,
};

const NoteControls = (props: Props) => (
  <div className={s.controls}>
    <DetailedInitials {...props.model.author} />
    <div className={s.buttons}>
      <DeleteButton className={s.button} onClick={() => props.onDeleteClick(props.model)} />
      <EditButton className={s.button} onClick={() => props.onEditClick(props.model)} />
    </div>
  </div>
);

export default NoteControls;
