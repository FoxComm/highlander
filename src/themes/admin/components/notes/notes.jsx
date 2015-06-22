'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import NoteStore from './store';
import { pluralize } from 'fleck';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const changeEvent = 'change-note-store';

export default class Notes extends React.Component {

  constructor(props) {
    super(props);
    this.onChangeNoteStore = this.onChangeNoteStore.bind(this);
    this.state = {
      notes: []
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

  addNote() {
    console.log('herere');
  }

  render() {
    let empty = null;
    if (!this.state.notes.length) {
      empty = <div className="empty">No notes yet.</div>;
    }

    return (
      <div id="notes">
        <h2>Notes</h2>
        <a onClick={this.addNote} className="add-note"> <i className="icon-plus"></i></a>
        <table>
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={this.state.notes} model='order'/>
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
    {field: 'body', text: 'Note'}
  ]
};
