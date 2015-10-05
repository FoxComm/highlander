'use strict';

import React from 'react';

export default class NoteForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: this.props.text || ''
    }
  }

  componentDidMount() {
    let node = React.findDOMNode(this.refs.textarea);
    let value = node.value;
    node.value = '';
    node.value = value;
    node.focus();
  }

  handleChange(event) {
    this.setState({value: event.target.value});
  }

  render() {
    let title = this.props.text ? 'Edit note' : 'New note';
    return (
      <div className="fc-notes-form">
        <form action={this.props.uri} method="post" onSubmit={this.props.onSubmit}>
          <fieldset>
            <legend>{title}</legend>
            <div className="note-body">
              <div className="counter">{this.state.value.length}/1000</div>
              <textarea
                name="body"
                maxLength="1000"
                ref="textarea"
                value={this.state.value}
                onChange={this.handleChange.bind(this)}
                required>
              </textarea>
            </div>
            <div className="fc-notes-form-controls">
              <input type="reset" onClick={this.props.onReset}/>
              <input type="submit" value="Save"/>
            </div>
          </fieldset>
        </form>
      </div>
    );
  }
}

NoteForm.propTypes = {
  uri: React.PropTypes.string,
  text: React.PropTypes.string,
  onReset: React.PropTypes.func,
  onSubmit: React.PropTypes.func
};
