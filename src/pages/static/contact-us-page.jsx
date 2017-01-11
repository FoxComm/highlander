/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { api } from 'lib/api';

// components
import PageTitle from '../../components/cms/page-title';
import { TextInput } from 'ui/inputs';
import { Form, FormField } from 'ui/forms';
import Button from 'ui/buttons';

// styles
import styles from './contact-us-page.css';

type State = {
  name: string,
  email: string,
  phone: string,
  subject: string,
  text: string,
};

class ContactUsPage extends Component {
  state: State = {

  };

  @autobind
  onFieldChange({ target }) {
    this.setState({
      [target.name]: target.value,
    });
  }

  @autobind
  sendMessage() {
    const { name, email, phone, subject, text } = this.state;

    api.post('/local/contact-feedback', {
      name, email, phone, subject, text,
    });
  }

  render() {
    const {
      name = '',
      email = '',
      phone = '',
      subject = '',
      text = '',
    } = this.state;

    return (
      <div>
        <PageTitle title="Contact Us" />
        <div styleName="content">
          <h2 styleName="subtitle">We'd love to hear from you</h2>
          <div styleName="contact-form">
            <Form onSubmit={this.sendMessage}>
              <FormField>
                <TextInput
                  styleName="input-field"
                  placeholder="First & last name"
                  name="name"
                  onChange={this.onFieldChange}
                  value={name}
                />
              </FormField>
              <FormField validator={null} required>
                <TextInput
                  styleName="input-field"
                  placeholder="Email address"
                  name="email"
                  onChange={this.onFieldChange}
                  value={email}
                />
              </FormField>
              <FormField>
                <TextInput
                  type="tel"
                  styleName="input-field"
                  placeholder="Phone"
                  name="phone"
                  onChange={this.onFieldChange}
                  value={phone}
                />
              </FormField>
              <FormField>
                <TextInput
                  styleName="input-field"
                  placeholder="Subject"
                  name="subject"
                  onChange={this.onFieldChange}
                  value={subject}
                />
              </FormField>
              <FormField required>
                <textarea
                  styleName="message-field"
                  placeholder="Your message"
                  name="text"
                  onChange={this.onFieldChange}
                  value={text}
                />
              </FormField>

              <Button
                styleName="submit-btn"
                type="submit"
                onClick={this.validateAndSubmit}
              >
                Submit
              </Button>
            </Form>
          </div>
        </div>
      </div>
    );
  }
}

export default ContactUsPage;
