//libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';

//components
import MultiSelectRow from '../table/multi-select-row';
import NoteControls from './controls';


const entityTitles = {
  rma: 'Return',
  order: 'Order',
  giftCard: 'GiftCard',
  customer: 'Customer'
};

const NoteRow = props => {
  const { note, columns, params, actions } = props;
  let cell = null;

  const setCellContents = (note, field) => {
    switch(field) {
      case 'author':
        return (
          <NoteControls
            model={note}
            onEditClick={(item) => actions.startEditingNote(item.id)}
            onDeleteClick={(item) => actions.startDeletingNote(item.id)}
          />
        );
      case 'transaction':
        if (note.referenceType != 'customer') {
          cell = (
            <div>
              <div>{entityTitles[note.referenceType] || note.referenceType}</div>
              <div>{note.referenceId}</div>
            </div>
          );
        }

        return cell;
      default:
        return _.get(note, field);
    }
  };

  return (
    <MultiSelectRow
      columns={columns}
      row={note}
      setCellContents={setCellContents}
      params={params} />
  );
};

NoteRow.propTypes = {
  note: PropTypes.object,
  columns: PropTypes.array,
  params: PropTypes.object,
  actions: PropTypes.object,
};

export default NoteRow;
