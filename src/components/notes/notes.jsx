'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
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
import { connect } from 'react-redux';
import * as NotesActinos from '../../modules/notes';
import { entityId } from '../../modules/state-helpers';

function mapStateToProps(state, {entity}) {
  return {
    notes: _.get(state.notes, [entity.entityType, entity.entityId, 'notes'], [])
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

  static propTypes = {
    tableColumns: PropTypes.array,
    entity: PropTypes.shape({
      entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
      entityType: PropTypes.string.isRequired
    })
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
    this.props.fetchNotesIfNeeded(this.props.entity);
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

    this.props.createNote(this.props.entity, data);
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
    if (this.state.editingNote && (this.state.editingNote.id === row.id)) {
      return (
        <TableRow key={`row-${index}`}>
          <TableCell colspan={3}>
            <NoteForm
              body={this.state.editingNote && this.state.editingNote.body}
              onReset={this.onResetForm}
              onSubmit={this.onEditFormSubmit}
            />
          </TableCell>
        </TableRow>
      );
    } else {
      return (
        <TableRow key={`row-${index}`}>
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
      );
    }
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
    // @TODO: re-enable this after Denys finished with table refactoring for redux
    // <TableView renderRow={this.renderNoteRow} empty={'No notes yet.'}/>

    return (
      <ContentBox title={'Notes'} actionBlock={this.controls}>
        {this.state.creatingNote && (
          <NoteForm
            onReset={this.onResetForm}
            onSubmit={this.onCreateFormSubmit}
            />
        )}
        <table>
          <tbody>
            {_.map(this.props.notes, this.renderNoteRow)}
          </tbody>
        </table>
      </ContentBox>
    );
  }
}
