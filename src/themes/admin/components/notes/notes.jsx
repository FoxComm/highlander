'use strict';

import React from 'react';
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
    console.log(notes);
    this.setState({notes: notes});
  }

  addNote() {
    console.log('herere');
  }

  render() {
    let notes = this.state.notes;

    return (
      <div id="notes">
        <h3>Notes</h3>
        <a onClick={this.addNote}> <i className="icon-plus"></i></a>
        <ul>
          {notes.map((note) => {
            return <li>{note.body}</li>;
          })}
        </ul>
      </div>
    );
  }
}

Notes.contextTypes = {
  router: React.PropTypes.func
};
