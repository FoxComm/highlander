'use strict';

import React from 'react';

export default class NoteForm extends React.Component {
  static propTypes = {
    uri: React.PropTypes.string,
    body: React.PropTypes.string,
    maxBodyLength: React.PropTypes.number,
    onReset: React.PropTypes.func,
    onSubmit: React.PropTypes.func
  };

  static defaultProps = {
    maxBodyLength: 1000
  };

  constructor(...args) {
    super(...args);
    this.state = {
      body: this.props.body || ''
    };
  }

  componentDidMount() {
    let node = this.refs.body;
    let value = node.value;
    node.value = '';
    node.value = value;
    node.focus();
  }

  handleChange(event) {
    this.setState({
      body: this.refs.body.value
    });
  }

  handleSubmit(event) {
    event.preventDefault();
    this.props.onSubmit({
      body: this.state.body
    });
  }

  render() {
    let title = this.props.body ? 'Edit note' : 'New note';
    return (
      <div className="fc-notes-form">
        <form onChange={this.handleChange.bind(this)} onSubmit={this.handleSubmit.bind(this)}>
          <fieldset>
            <legend>{title}</legend>
            <div className="note-body">
              <div className="counter">{this.state.body.length}/1000</div>
              <textarea
                ref="body"
                name="body"
                maxLength={this.props.maxBodyLength}
                value={this.state.body}
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
