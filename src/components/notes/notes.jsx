'use strict';

import React from 'react';
import Api from '../../lib/api';
import Wrapper from '../wrapper/wrapper';
import Panel from '../panel/panel';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import Controls from './controls';
import Form from './form';
import NoteStore from './store';
import UserInitials from '../users/initials';
import ConfirmModal from '../modal/confirm';
import { dispatch } from '../../lib/dispatcher';

const deleteOptions = {
  header: 'Confirm',
  body: 'Are you sure you want to delete this note?',
  proceed: 'Yes',
  cancel: 'No'
};

export default class Notes extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      creatingNote: false,
      editingNote: null
    };
  }

  componentDidMount() {
    let model = this.props.modelName;
    if (model === 'order') {
      NoteStore.uriRoot = `/notes/${model}/${this.props[model].referenceNumber}`;
    } else if (model === 'gift-card') {
      NoteStore.uriRoot = `/notes/${model}/${this.props[model].code}`;
    } else {
      NoteStore.uriRoot = `/notes/${model}/${this.props[model].id}`;
    }

    NoteStore.listenToEvent('change', this);
    NoteStore.fetch();
  }

  onChangeNoteStore(notes) {
    this.setState({notes: notes});
  }

  componentWillUnmount() {
    NoteStore.stopListeningToEvent('change', this);
  }

  handleEdit(item) {
    this.setState({
      creatingNote: false,
      editingNote: item
    });
  }

  handleDelete(item) {
    this.confirmDeleteNote(item);
  }

  handleResetForm() {
    this.setState({
      creatingNote: false,
      editingNote: null
    });
  }

  handleCreateForm(data) {
    Api.post(`${NoteStore.baseUri}`, data)
      .then((note) => {
        this.toggleCreating();
      })
      .catch((err) => {
        console.error(err);
      });
  }

  handleEditForm(data) {
    Api.patch(`${NoteStore.baseUri}/${this.state.editingNote.id}`, data)
      .then((note) => {
        this.setState({
          editingNote: null
        })
      })
      .catch((err) => {
        console.error(err);
      });
  }

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
    dispatch('toggleModal', <ConfirmModal callback={this.onConfirmDeleteNote.bind(this)} details={deleteOptions}/>);
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
    NoteStore.delete(this.state.deletingNote.id);
  }

  render() {
    let renderRow = (row, index) => {
      return (
        <Wrapper>
          <TableRow>
            <TableCell>Today</TableCell>
            <TableCell>{row.body}</TableCell>
            <TableCell>
              <Controls
                model={row}
                onEditClick={this.handleEdit.bind(this)}
                onDeleteClick={this.handleDelete.bind(this)}
                />
            </TableCell>
          </TableRow>
          {this.state.editingNote && (this.state.editingNote.id === row.id) && (
            <TableRow>
              <TableCell colspan={3}>
                <Form
                  uri={NoteStore.baseUri}
                  text={this.state.editingNote && this.state.editingNote.body}
                  onReset={this.handleResetForm.bind(this)}
                  onSubmit={this.handleEditForm.bind(this)}
                  />
              </TableCell>
            </TableRow>
          )}
        </Wrapper>
      );
    };

    let controls = (
      <Wrapper>
        <button onClick={this.toggleCreating.bind(this)} disabled={!!this.state.creatingNote}>
          <i className="icon-add"></i>
        </button>
      </Wrapper>
    );

    return (
      <Panel title={'Notes'} controls={controls}>
        {this.state.creatingNote && (
          <Form
            uri={NoteStore.baseUri}
            onReset={this.handleResetForm.bind(this)}
            onSubmit={this.handleCreateForm.bind(this)}
            />
        )}
        {NoteStore.rows.length && (
          <TableView store={NoteStore} renderRow={renderRow.bind(this)}/>
        )}
        {!NoteStore.rows.length && (
          <div className="empty">No notes yet.</div>
        )}
      </Panel>
    );
  }
}

Notes.propTypes = {
  tableColumns: React.PropTypes.array,
  order: React.PropTypes.object,
  modelName: React.PropTypes.string
};
