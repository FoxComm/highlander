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
import { pluralize } from 'fleck';

export default class Notes extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      notes: [],
      creating: false,
      editing: false,
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

  componentWillUnmount() {
    NoteStore.stopListeningToEvent('change', this);
  }

  onChangeNoteStore(notes) {
    this.setState({notes: notes});
  }

  handleEdit(item) {
    this.setState({
      creating: false,
      editing: true,
      editingNote: item
    });
  }

  handleDelete(item) {
    NoteStore.delete(item.id);
  }

  handleResetForm() {
    this.setState({
      creating: false,
      editing: false,
      editingNote: null
    });
  }

  handleSubmit(event) {
    event.preventDefault();
    Api.submitForm(event.target)
      .then((note) => {
        this.toggleCreating();
      })
      .catch((err) => {
        console.error(err);
      });
  }

  toggleCreating() {
    this.setState({
      creating: !this.state.creating,
      editing: this.state.editing && this.state.creating
    });
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
          {this.state.editing && (this.state.editingNote.id === row.id) && (
            <TableRow>
              <TableCell colspan={3}>
                <Form
                  uri={NoteStore.baseUri}
                  text={this.state.editingNote && this.state.editingNote.body}
                  onReset={this.handleResetForm.bind(this)}
                  onSubmit={this.handleSubmit.bind(this)}
                  />
              </TableCell>
            </TableRow>
          )}
        </Wrapper>
      );
    };

    let controls = (
      <Wrapper>
        <button onClick={this.toggleCreating.bind(this)} disabled={!!this.state.creating}>
          <i className="icon-add"></i>
        </button>
      </Wrapper>
    );

    return (
      <Panel title={'Notes'} controls={controls}>
        {this.state.creating && (
          <Form
            uri={NoteStore.baseUri}
            onReset={this.handleResetForm.bind(this)}
            onSubmit={this.handleSubmit.bind(this)}
            />
        )}
        {this.state.notes.length && (
          <TableView store={NoteStore} renderRow2={renderRow.bind(this)}/>
        )}
        {!this.state.notes.length && (
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
