'use strict';

import React from 'react';

class Notes extends React.Component {
  addNote() {
    console.log('herere');
  }

  render() {
    <div id="notes">
      <h3>Notes</h3>
      <a onClick={this.addNote}> <i class="icon-plus"></i></a>
    </div>
  }
}

export default Notes;
