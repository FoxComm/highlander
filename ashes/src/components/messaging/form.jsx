// libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import SaveCancel from '../common/save-cancel';

import Form from 'components/forms/form';
import FormField from 'components/forms/formfield';

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
  handleSubjectChange({target}) {
    this.setState({
      subject: target.value,
    });
  }

  @autobind
  handleSubmit(event) {
    event.preventDefault();
    this.props.onSubmit({
      subject: this.state.subject,
      body: this.state.body
    });
  }

  render() {
    const { state, props } = this;
    const title = props.body ? 'Edit note' : 'New message';

    return (
      <div className="fc-notes-form">
        <Form onSubmit={this.handleSubmit}>
          <fieldset>
            <FormField label="Subject" validator="ascii" maxLength={255} required>
              <input 
                type="text" 
                ref="subject" 
                name="subject" 
                value={state.subject} 
                onChange={this.handleSubjectChange} 
                required 
              />
            </FormField>
            <div className="note-body">
              <FormField label="Body" validator="ascii" maxLength={255} required>
                <textarea
                  ref="body"
                  name="body"
                  maxLength={props.maxBodyLength}
                  value={state.body}
                  onChange={this.handleChange}
                  required
                />
              </FormField>
            </div>
            <div className="fc-notes-form-controls">
              <SaveCancel
                onCancel={props.onReset}
              />
            </div>
          </fieldset>
        </Form>
      </div>
    );
  }
}
