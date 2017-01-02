// libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import SaveCancel from '../common/save-cancel';

export default class NoteForm extends React.Component {
  static propTypes = {
    body: PropTypes.string,
    maxBodyLength: PropTypes.number,
    onReset: PropTypes.func,
    onSubmit: PropTypes.func
  };

  static defaultProps = {
    maxBodyLength: 1000
  };

  state = {
    body: this.props.body || '',
  };

  componentDidMount() {
    const node = this.refs.body;
    const value = node.value;
    node.value = '';
    node.value = value;
    node.focus();
  }


  @autobind
  handleChange({target}) {
    this.setState({
      body: target.value,
    });
  }

  @autobind
  handleSubmit(event) {
    event.preventDefault();
    this.props.onSubmit({
      body: this.state.body
    });
  }

  render() {
    const { state, props } = this;
    const title = props.body ? 'Edit note' : 'New message';

    return (
      <div className="fc-notes-form">
        <form onSubmit={this.handleSubmit}>
          <fieldset>
            <legend>{title}</legend>
            <div className="note-body">
              <div className="counter">{state.body.length}/1000</div>
              <textarea
                ref="body"
                name="body"
                maxLength={props.maxBodyLength}
                value={state.body}
                onChange={this.handleChange}
                required
              />
            </div>
            <div className="fc-notes-form-controls">
              <SaveCancel
                onCancel={props.onReset}
              />
            </div>
          </fieldset>
        </form>
      </div>
    );
  }
}
