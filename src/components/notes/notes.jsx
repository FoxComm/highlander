'use strict';

import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import NotesItemControls from './notesItemControls';
import NoteStore from './store';
import { pluralize } from 'fleck';

export default class Notes extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      notes: [],
      open: false,
      count: 0
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

  handleChange(event) {
    this.setState({count: event.target.value.length});
  }

  handleSubmit(event) {
    event.preventDefault();
    Api.submitForm(event.target)
      .then((note) => {
        note.isNew = true;
        let notes = this.state.notes.slice(0);
        notes.unshift(note);
        this.setState({notes: notes});
        this.toggleNote();
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

  toggleNote() {
    this.setState({open: !this.state.open, count: 0});
  }

  render() {
    let empty = null;
    if (!this.state.notes.length) {
      empty = <div className="is-empty">No notes yet.</div>;
    }

    return (
      <div className="fc-notes">
        <h2 className="fc-notes-header">Notes</h2>
        <button onClick={this.toggleNote.bind(this)} className={`fc-notes-add fc-btn fc-btn-primary ${this.state.open ? 'is-hidden' : null }`}>
          <i className="icon-add"></i>
        </button>

        <form action={NoteStore.baseUri} method="post" onSubmit={this.handleSubmit.bind(this)} className={`fc-notes-form ${this.state.open ? 'is-shown' : null}`}>
          <fieldset>
            <legend>New Note</legend>
            <div className="fc-notes-body">
              <div className="fc-counter">{this.state.count}/1000</div>
              <textarea name="body" maxLength="1000" onChange={this.handleChange.bind(this)} required></textarea>
            </div>
            <div>
              <input type="reset" value="&times;" onClick={this.toggleNote.bind(this)}/>
              <input type="submit" value="Save"/>
            </div>
          </fieldset>
        </form>
        <table className="fc-table fc-table-expanded">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={this.state.notes} model={this.props.modelName}>
            <NotesItemControls/>
          </TableBody>
        </table>
        {empty}
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
    {field: 'author', text: 'Author', component: 'NotesItemControls'}
  ]
};
