'use strict';

import React from 'react';

export default class NoteForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: ''
    }
  }

  handleChange(event) {
    this.setState({value: event.target.value});
  }

  render() {
    return (
      <form action={this.props.uri} method="post" onSubmit={this.props.onSubmit}>
        <fieldset>
          <legend>New Note</legend>
          <div className="note-body">
            <div className="counter">{this.state.value.length}/1000</div>
            <textarea name="body" maxLength="1000" onChange={this.handleChange.bind(this)} required></textarea>
          </div>
          <div>
            <input type="submit" value="Save"/>
          </div>
        </fieldset>
      </form>
    );
  }
}

NoteForm.propTypes = {
  uri: React.PropTypes.string,
  onSubmit: React.PropTypes.func
};
