'use strict';

import _ from 'lodash';
import React from 'react';
import { autobind } from 'core-decorators';
import ContentBox from '../content-box/content-box';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import NoteControls from './controls';
import NoteForm from './form';
import UserInitials from '../users/initials';
import DateTime from '../datetime/datetime';
import ConfirmModal from '../modal/confirm';
import * as NotesActinos from '../../modules/notes';
import { modelIdentity } from '../../modules/state-helpers';

function mapStateToProps(state, props) {
  const model = props[props.modelName];
  const identity = modelIdentity(props.modelName, model);

  return {
    notes: _.get(state.notes, [props.modelName, identity, 'notes'], [])
  };
}

@connect(mapStateToProps, NotesActinos)
export default class Notes extends React.Component {
  static deleteOptions = {
    header: 'Confirm',
    body: 'Are you sure you want to delete this note?',
    proceed: 'Yes',
    cancel: 'No'
  };

  constructor(...args) {
    super(...args);
    this.state = {
      creatingNote: false,
      editingNote: null,
      deletingNote: null
    };
  }

  componentDidMount() {
    const type = this.props.modelName;
    const model = this.props[type];

    this.props.fetchNotesIfNeeded(type, model);
  }

  @autobind
  onEditNote(item) {
    this.setState({
      creatingNote: false,
      editingNote: item
    });
  }

  @autobind
  onDeleteNote(item) {
    this.confirmDeleteNote(item);
  }

  @autobind
  onResetForm() {
    this.setState({
      creatingNote: false,
      editingNote: null
    });
  }

  @autobind
  onCreateFormSubmit(data) {
    this.setState({
      creatingNote: false
    });
    // NotesStore.create(data);
  }

  onEditFormSubmit(data) {
    this.setState({
      editingNote: false
    });
    // NotesStore.patch(this.state.editingNote.id, data);
  }

  @autobind
  toggleCreating() {
    this.setState({
      creatingNote: !this.state.creatingNote,
      editingNote: this.state.creating && this.state.editingNote
    });
  }

  confirmDeleteNote(item) {
    this.setState({
      deletingNote: item
    });
    dispatch('toggleModal', (
      <ConfirmModal callback={this.onConfirmDeleteNote.bind(this)} details={this.deleteOptions}/>
    ));
  }

  onConfirmDeleteNote(success) {
    if (success) {
      this.deleteNote();
    } else {
      this.setState({
        deletingNote: null
      });
    }
  }

  deleteNote() {
    // NotesStore.delete(this.state.deletingNote.id);
  }

  @autobind
  renderNoteRow(row, index) {
    return (
      <div>
        {(this.state.editingNote && (this.state.editingNote.id === row.id) && (
        <TableRow>
          <TableCell colspan={3}>
            <NoteForm
              body={this.state.editingNote && this.state.editingNote.body}
              onReset={this.onResetForm}
              onSubmit={this.onEditFormSubmit}
            />
          </TableCell>
        </TableRow>
          )) || (
        <TableRow>
          <TableCell>
            <DateTime value={row.createdAt}/>
          </TableCell>
          <TableCell>
            {row.body}
          </TableCell>
          <TableCell>
            <NoteControls
              model={row}
              onEditClick={this.onEditNote}
              onDeleteClick={this.onDeleteNote}
            />
          </TableCell>
        </TableRow>
          )}
      </div>
    );
  }

  get controls() {
    return (
      <button
        className="fc-btn fc-btn-primary"
        onClick={this.toggleCreating}
        disabled={!!this.state.creatingNote}
      >
        <i className="icon-add"></i>
      </button>
    );
  }

  render() {
    return (
      <ContentBox title={'Notes'} actionBlock={this.controls}>
        {this.state.creatingNote && (
          <NoteForm
            onReset={this.onResetForm}
            onSubmit={this.onCreateFormSubmit}
            />
        )}
        /* @TODO: re-enable this after Denys finished with table refactoring for redux
        <TableView renderRow={this.renderNoteRow} empty={'No notes yet.'}/>
        */
      </ContentBox>
    );
  }
}

Notes.propTypes = {
  tableColumns: React.PropTypes.array,
  order: React.PropTypes.object,
  modelName: React.PropTypes.string
};
