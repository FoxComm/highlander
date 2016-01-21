// libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

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

  @autobind
  handleChange() {
    this.setState({
      body: this.refs.body.value
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
    let title = this.props.body ? 'Edit note' : 'New note';
    const {body} = this.state;
    const {maxBodyLength, onReset} = this.props;

    return (
      <div className="fc-notes-form">
        <form onChange={this.handleChange} onSubmit={this.handleSubmit}>
          <fieldset>
            <legend>{title}</legend>
            <div className="note-body">
              <div className="counter">{this.state.body.length}/1000</div>
              <textarea
                ref="body"
                name="body"
                maxLength={maxBodyLength}
                value={body}
                required>
              </textarea>
            </div>
            <div className="fc-notes-form-controls">
              <input className="fc-btn fc-btn-secondary" type="reset" value="Cancel" onClick={onReset}/>
              <input className="fc-btn fc-btn-primary" type="submit" value="Save"/>
            </div>
          </fieldset>
        </form>
      </div>
    );
  }
}
