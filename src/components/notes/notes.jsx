import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { createSelector } from 'reselect';
import { assoc } from 'sprout-data';
import { connect } from 'react-redux';

import ConfirmationDialog from '../modal/confirmation-dialog';
import { PrimaryButton } from '../../components/common/buttons';
import { SectionTitle } from '../section-title';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import NoteControls from './controls';
import NoteForm from './form';
import { DateTime } from '../common/datetime';
import ConfirmModal from '../modal/confirm';

import * as NotesActinos from '../../modules/notes';
import { entityId } from '../../modules/state-helpers';


const entityTitles = {
  rma: 'Return',
  order: 'Order',
  giftCard: 'GiftCard',
  customer: 'Customer'
};

const editingNote = createSelector(
  (state, entity) => _.get(state.notes, [entity.entityType, entity.entityId, 'rows'], []),
  (state, entity) => _.get(state.notes, [entity.entityType, entity.entityId, 'editingNoteId']),
  (notes, editingNoteId) => {
    return _.findWhere(notes, {id: editingNoteId});
  }
);

function mapStateToProps(state, {entity}) {
  const notesData = _.get(state.notes, [entity.entityType, entity.entityId], {rows: []});

  return assoc(
    notesData,
    'editingNote', editingNote(state, entity),
    'data', notesData
  );
}

function mapDispatchToProps(dispatch, props) {
  return _.transform(NotesActinos, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(props.entity, ...args));
    };
  });
}

/*eslint "react/prop-types": 0*/

@connect(mapStateToProps, mapDispatchToProps)
export default class Notes extends React.Component {
  static deleteOptions = {
    header: 'Confirm',
    body: 'Are you sure you want to delete this note?',
    confirm: 'Yes',
    cancel: 'No'
  };

  static propTypes = {
    entity: PropTypes.shape({
      entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
      entityType: PropTypes.string.isRequired
    })
  };

  componentDidMount() {
    this.props.fetchNotes();
  }

  get isCustomerNotes() {
    return this.props.entity.entityType == 'customer';
  }

  get tableColumns() {
    let baseColumns = [
      {field: 'body', text: 'Note'},
      {field: 'author', text: 'Author'}
    ];
    if (this.isCustomerNotes) {
      baseColumns = [
        {field: null, text: 'Transaction'},
        ...baseColumns,
      ];
    }

    return [
      {field: 'createdAt', text: 'Date/Time'},
      ...baseColumns
    ];
  }

  @autobind
  renderNoteRow(row, index, isNew) {
    if (this.props.editingNoteId === row.id) {
      return (
        <TableRow key={`row-${index}`}>
          <TableCell colspan={this.tableColumns.length}>
            <NoteForm
              body={this.props.editingNote && this.props.editingNote.body}
              onReset={this.props.stopAddingOrEditingNote}
              onSubmit={data => this.props.editNote(this.props.editingNoteId, data)}
            />
          </TableCell>
        </TableRow>
      );
    } else {
      let transaction = null;
      if (this.isCustomerNotes) {
        let cell = null;
        if (row.referenceType != 'customer') {
          cell = (
            <div>
              <div>{entityTitles[row.referenceType] || row.referenceType}</div>
              <div>{row.referenceId}</div>
            </div>
          );
        }
        transaction = (
          <TableCell>
            {cell}
          </TableCell>
        );
      }
      return (
        <TableRow key={`row-${index}`} isNew={isNew}>
          <TableCell>
            <DateTime value={row.createdAt}/>
          </TableCell>
          {transaction}
          <TableCell>
            {row.body}
          </TableCell>
          <TableCell>
            <NoteControls
              model={row}
              onEditClick={(item) => this.props.startEditingNote(item.id)}
              onDeleteClick={(item) => this.props.startDeletingNote(item.id)}
            />
          </TableCell>
        </TableRow>
      );
    }
  }

  @autobind
  injectAddingForm(rows) {
    // -1 means we want add new note
    if (this.props.editingNoteId === -1) {
      return [
        <TableRow key="row-add">
          <TableCell colspan={this.tableColumns.length}>
            <NoteForm
              onReset={this.props.stopAddingOrEditingNote}
              onSubmit={this.props.createNote}
            />
          </TableCell>
        </TableRow>,
        ...rows
      ];
    }
    return rows;
  }

  get controls() {
    return (
      <PrimaryButton icon="add" onClick={this.props.startAddingNote} disabled={!!this.props.isAddingNote } />
    );
  }

  get sectionClassName() {
    const entityType = this.props.entity.entityType;
    return `fc-${entityType}-notes`;
  }

  render() {
    return (
      <div className={this.sectionClassName} >
        <SectionTitle className="fc-grid-gutter fc-notes-section-title" title="Notes">{this.controls}</SectionTitle>
        <TableView
          renderRow={this.renderNoteRow}
          processRows={this.injectAddingForm}
          detectNewRows={this.props.wasReceived}
          columns={this.tableColumns}
          data={this.props.data}
          setState={this.props.fetchNotes}
          emptyMessage="No notes yet."
        />
        <ConfirmationDialog
          {...Notes.deleteOptions}
          isVisible={this.props.noteIdToDelete != null}
          confirmAction={() => this.props.deleteNote(this.props.noteIdToDelete)}
          cancelAction={() => this.props.stopDeletingNote(this.props.noteIdToDelete)}
        />
      </div>
    );
  }
}
