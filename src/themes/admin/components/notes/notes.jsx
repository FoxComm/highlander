'use strict';

import React from 'react';
import Api from '../../lib/api';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import UserInitials from '../users/initials';
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
    NoteStore.uriRoot = `${pluralize(model)}/${this.props[model].id}`;
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
      .catch((err) => { console.log(err); });
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
      empty = <div className="empty">No notes yet.</div>;
    }

    return (
      <div id="notes">
        <h2>Notes</h2>
        <a onClick={this.toggleNote.bind(this)} className="add-note" disabled={this.state.open}><i className="icon-plus"></i></a>
        <form action={NoteStore.baseUri} method="post" onSubmit={this.handleSubmit.bind(this)}>
          <fieldset>
            <legend>New Note</legend>
            <div className="note-body">
              <div className="counter">{this.state.count}/1000</div>
              <textarea name="body" maxLength="1000" onChange={this.handleChange.bind(this)} required></textarea>
            </div>
            <div>
              <input type="reset" value="&times;" onClick={this.toggleNote.bind(this)}/>
              <input type="submit" value="Save"/>
            </div>
          </fieldset>
        </form>
        <table>
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={this.state.notes} model='order'>
            <UserInitials/>
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
    {field: 'createdAt', text: 'Date/Time', type: 'date', format: 'MM/DD/YYYY h:mm a'},
    {field: 'body', text: 'Note'},
    {field: 'author', text: 'Author', component: 'UserInitials'},
    {field: 'isEditable', text: ''}
  ]
};
