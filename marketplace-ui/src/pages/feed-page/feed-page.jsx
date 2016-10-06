/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';

import {
  getApplication,
  getApplicationFetched,
  getApplicationFetchFailed,
  getFeedSubmitInProgress,
  getFeedSubmitFailed,
} from '../../core/modules';
import { fetch as fetchApplication, clearErrors } from '../../core/modules/merchant-application';

import { fields, initialValues } from '../../forms/feed/feed-fields';

import styles from './feed-page.css';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';

type Props = {
  params: Object;
  application: Application;
  applicationFetched: boolean;
  applicationFetchFailed: boolean;
  fetchApplication: (reference: string) => Promise<*>;
  clearErrors: () => void;
  replace: (path: string) => void;
}


class FeedPage extends Component {
  props: Props;

  componentWillMount(): void {
    const {
      fetchApplication,
      params: { ref },
      applicationFetched,
      applicationFetchFailed,
      clearErrors,
      replace,
    } = this.props;

    if (!applicationFetched) {
      fetchApplication(ref);
    }

    if (applicationFetchFailed) {
      clearErrors();
      replace('/application');
    }
  }

  get form(): HTMLElement {
    const { inProgress, failed } = this.props;

    return (
      <Form
        form="feed"
        onChange={this.onChange}
        fields={fields}
        initialValues={initialValues}
        onSubmit={this.submit}
        inProgress={inProgress}
        failed={failed}
      />
    );
  }

  render(): HTMLElement {
    return (
      <div className={styles.feed}>
        <Header
          title="Add your product feed"
          legend={`If you have an existing Google Product or Amazon Product feed, we can automatically import
          your products. Or you can select to manually supply your products via an .XML, .CSV, or .TXT files.`}
        />
        {this.form}
      </div>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  applicationFetched: getApplicationFetched(state),
  applicationFetchFailed: getApplicationFetchFailed(state),
  inProgress: getFeedSubmitInProgress(state),
  failed: getFeedSubmitFailed(state),
});

export default connect(mapState, { fetchApplication, clearErrors, replace })(FeedPage);
