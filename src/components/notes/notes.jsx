'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import ConfirmationDialog from '../modal/confirmation-dialog';
import { PrimaryButton } from '../../components/common/buttons';
import SectionTitle from '../section-title/section-title';
import ContentBox from '../content-box/content-box';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import NoteControls from './controls';
import NoteForm from './form';
import UserInitials from '../users/initials';
import { DateTime } from '../common/datetime';
import ConfirmModal from '../modal/confirm';
import { connect } from 'react-redux';
import * as NotesActinos from '../../modules/notes';
import { entityId } from '../../modules/state-helpers';
import { createSelector } from 'reselect';
import { assoc } from 'sprout-data';

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
    tableColumns: PropTypes.array,
    entity: PropTypes.shape({
      entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
      entityType: PropTypes.string.isRequired
    })
  };

  static defaultProps = {
    tableColumns: [
      {field: 'createdAt', text: 'Date/Type'},
      {field: 'body', text: 'Body'},
      {field: 'author', text: 'Author'}
    ]
  };

  componentDidMount() {
    this.props.fetchNotes();
  }

  @autobind
  renderNoteRow(row, index, isNew) {
    if (this.props.editingNoteId === row.id) {
      return (
        <TableRow key={`row-${index}`}>
          <TableCell colspan={3}>
            <NoteForm
              body={this.props.editingNote && this.props.editingNote.body}
              onReset={this.props.stopAddingOrEditingNote}
              onSubmit={data => this.props.editNote(this.props.editingNoteId, data)}
            />
          </TableCell>
        </TableRow>
      );
    } else {
      return (
        <TableRow key={`row-${index}`} isNew={isNew}>
          <TableCell>
            <DateTime value={row.createdAt}/>
          </TableCell>
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
    if (this.props.editingNoteId === true) {
      return [
        <TableRow key="row-add">
          <TableCell colspan={this.props.tableColumns.length}>
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

  render() {
    return (
      <div>
        <SectionTitle className="fc-grid-gutter" title="Notes">{this.controls}</SectionTitle>
        <TableView
          renderRow={this.renderNoteRow}
          processRows={this.injectAddingForm}
          detectNewRows={this.props.wasReceived}
          columns={this.props.tableColumns}
          data={this.props.data}
          setState={(data, params) => this.props.fetchNotes(params)}
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
