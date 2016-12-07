/* @flow */

// libs
import React, { Component } from 'react';

// components
import PageTitle from '../../components/cms/page-title';
import PageBody from '../../components/cms/page-body';

import data from './contact-us-data.json';

class ContactUsPage extends Component {
  render() {
    return (
      <div>
        <PageTitle title="Contact Us" />
        <PageBody blocks={data} />
      </div>
    );
  }
}

export default ContactUsPage;