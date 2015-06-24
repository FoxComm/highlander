'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import UserInitials from '../users/initials';
import NoteStore from './store';
import { pluralize } from 'fleck';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const changeEvent = 'change-note-store';

export default class Notes extends React.Component {

  constructor(props) {
    super(props);
    this.onChangeNoteStore = this.onChangeNoteStore.bind(this);
    this.state = {
      notes: [],
      open: false
    };
  }

  componentDidMount() {
    let
      { router }  = this.context,
      params      = router.getCurrentParams(),
      model       = Object.keys(params)[0];

    NoteStore.uriRoot = `${pluralize(model)}/${params[model]}`;
    listenTo(changeEvent, this);
    NoteStore.fetch();
  }

  componentWillUnmount() {
    stopListeningTo(changeEvent, this);
  }

  onChangeNoteStore(notes) {
    this.setState({notes: notes});
  }

  toggleNote() {
    this.setState({open: !this.state.open});
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
        <form action={NoteStore.baseUri} method="post">
          <formset>
            <legend>New Note</legend>
            <div className="note-body">
              <textarea name="body" required></textarea>
            </div>
            <div>
              <input type="reset" value="&times;" onClick={this.toggleNote.bind(this)}/>
              <input type="submit" value="Save"/>
            </div>
          </formset>
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

Notes.contextTypes = {
  router: React.PropTypes.func
};

Notes.propTypes = {
  tableColumns: React.PropTypes.array
};

Notes.defaultProps = {
  tableColumns: [
    {field: 'createdAt', text: 'Date/Time', type: 'date', format: 'MM/DD/YYYY h:mm A'},
    {field: 'body', text: 'Note'},
    {field: 'author', text: 'Author', component: 'UserInitials'},
    {field: 'isEditable', text: ''}
  ]
};
