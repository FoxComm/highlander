'use strict';

import React from 'react';
import Api from '../../lib/api';
import TableView from '../tables/tableview';
import NotesItemControls from './controls';
import Form from './form';
import NoteStore from './store';
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
      NoteStore.uriRoot = `${pluralize(model)}/${this.props[model].referenceNumber}`;
    } else if (model === 'gift-card') {
      NoteStore.uriRoot = `${pluralize(model)}/${this.props[model].code}`;
    } else {
      NoteStore.uriRoot = `${pluralize(model)}/${this.props[model].id}`;
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
        note.isNew = true;
        let notes = this.state.notes.slice(0);
        notes.unshift(note);
        this.setState({notes: notes});
        this.toggleCreating();
        this.removeNew();
      })
      .catch((err) => {
        console.error(err);
      });
  }

  removeNew() {
    setTimeout(() => {
      let row = document.querySelector('tr.new');
      row.classList.remove('new');
    }, 5000);
  }

  toggleCreating() {
    this.setState({
      creating: !this.state.creating,
      editing: this.state.editing && this.state.creating
    });
  }

  render() {
    return (
      <div id="notes" className="fc-content-box">
        <header className="header">
          <h2>Notes</h2>

          <div className="fc-content-box-controls">
            <button className="fc-notes-create"
                    onClick={this.toggleCreating.bind(this)}
                    disabled={!!this.state.creating}>
              <i className="icon-add"></i>
            </button>
          </div>
        </header>

        {this.state.creating && (
          <Form
            uri={NoteStore.baseUri}
            onReset={this.handleResetForm.bind(this)}
            onSubmit={this.handleSubmit.bind(this)}
            />
        )}

        {this.state.editing && (
          <Form
            uri={NoteStore.baseUri}
            text={this.state.editingNote && this.state.editingNote.body}
            onReset={this.handleResetForm.bind(this)}
            onSubmit={this.handleSubmit.bind(this)}
            />
        )}

        {this.state.notes.length && (
          <TableView
            columns={this.props.tableColumns}
            rows={this.state.notes}
            model={this.props.modelName}
            sort={NoteStore.sort.bind(NoteStore)}
            >
            <NotesItemControls
              onEditClick={this.handleEdit.bind(this)}
              onDeleteClick={this.handleDelete.bind(this)}
              />
          </TableView>
        )}
        {!this.state.notes.length && (
          <div className="empty">No notes yet.</div>
        )}
      </div>
    );
  }
}

Notes.propTypes = {
  tableColumns: React.PropTypes.array,
  order: React.PropTypes.object,
  modelName: React.PropTypes.string
};

Notes.defaultProps = {
  tableColumns: [
    {field: 'createdAt', text: 'Date/Time', type: 'date'},
    {field: 'body', text: 'Note'},
    {field: '', text: 'Author', component: 'NotesItemControls'}
  ]
};
