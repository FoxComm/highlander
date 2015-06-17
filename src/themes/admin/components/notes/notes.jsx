'use strict';

import React from 'react';

export default class Notes extends React.Component {
  addNote() {
    console.log('herere');
  }

  render() {
    let { router } = this.context;
    let id = router.getCurrentParams().order;
    return (
      <div id="notes">
        <h3>Notes {id}</h3>
        <a onClick={this.addNote}> <i className="icon-plus"></i></a>
      </div>
    );
  }
}

Notes.contextTypes = {
  router: React.PropTypes.func
};
