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
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import Loader from 'ui/loader';

// styles
import styles from './contact-us-page.css';

type State = {
  name: string,
  email: string,
  phone: string,
  subject: string,
  text: string,
  sending?: boolean,
  sent?: boolean,
  error?: any,
};

class ContactUsPage extends Component {
  state: State = {
    name: '',
    email: '',
    phone: '',
    subject: '',
    text: '',
    sending: false,
    sent: false,
    error: null,
  };

  @autobind
  onFieldChange({ target }: any) {
    this.setState({
      [target.name]: target.value,
    });
  }

  @autobind
  sendMessage() {
    const { name, email, phone, subject, text } = this.state;

    this.setState({ sending: true, sent: false, error: null });
    api
      .post('/local/contact-feedback', {
        name, email, phone, subject, text,
      })
      .then(() => {
        this.setState({ sent: true });
      })
      .catch(error => {
        this.setState({ error });
      })
      .then(() => {
        this.setState({ sending: false });
      });
  }

  renderForm() {
    const {
      name = '',
      email = '',
      phone = '',
      subject = '',
      text = '',
      error,
      sending,
    } = this.state;

    return (
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
          <FormField required>
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

          {error &&
            <div styleName="error">
              <ErrorAlerts error={error} />
            </div>}

          {sending &&
            <Loader size="m" />}

          {!sending &&
            <Button styleName="submit-btn" type="submit">
              Submit
            </Button>}
        </Form>
      </div>
    );
  }

  renderMessagePreview() {
    const {
      name,
      email,
      phone,
      subject,
      text,
    } = this.state;

    return (
      <div styleName="message-preview">
        <h2 styleName="sent-info">Your message has been sent successfully.</h2>
        <div>Name: {name}</div>
        <div>Email: {email}</div>
        <div>Phone: {phone}</div>
        <div>Subject: {subject}</div>
        <div>Message:</div>
        <div>{text}</div>
      </div>
    );
  }

  render() {
    const { sent } = this.state;
    const content = sent ?
      this.renderMessagePreview() :
      this.renderForm();

    return (
      <div>
        <PageTitle title="Contact Us" />
        <div styleName="content">
          <h2 styleName="subtitle">We'd love to hear from you</h2>
          {content}
        </div>
        <div styleName="contacts">
          <div styleName="contact-title">Phone</div>
          <div styleName="phone">(866) 461 - 4183</div>
          <div styleName="contact-title">Address</div>
          <div styleName="contact-data">
            <div>8012 Bellona Ave Suite B</div>
            <div>Towson, MD 21204</div>
          </div>
        </div>
      </div>
    );
  }
}

export default ContactUsPage;
