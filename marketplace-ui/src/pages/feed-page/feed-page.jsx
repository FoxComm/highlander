/* @flow */

import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import FeedForm from '../../forms/feed/feed-form';
import UploadForm from '../../forms/feed/upload-form';

import {
  getApplication,
  getApplicationFetched,
  getApplicationFetchFailed,
  getFeedSubmitInProgress,
  getFeedSubmitFailed,
  getFeedUploadInProgress,
  getFeedUploadFailed,
} from '../../core/modules';
import { fetch as fetchApplication, clearErrors } from '../../core/modules/merchant-application';
import { submit, upload } from '../../core/modules/products-feed';

import { fields as feedFields, initialValues } from '../../forms/feed/feed-fields';
import { fields as uploadFields } from '../../forms/feed/upload-fields';

import styles from './feed-page.css';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';

type Props = {
  params: Object;
  feedSubmitInProgress: boolean;
  feedSubmitFailed: boolean;
  feedUploadInProgress: boolean;
  feedUploadFailed: boolean;
  application: Application;
  applicationFetched: boolean;
  applicationFetchFailed: boolean;
  fetchApplication: (reference: string) => Promise<*>;
  submit: (data: Object) => Promise<*>;
  upload: (data: Object) => Promise<*>;
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

  @autobind
  submit(data) {
    const merchantId = get(this.props.application, 'merchant.id');

    if (!merchantId) {
      console.error('No merchantId');

      return;
    }

    return this.props.submit(merchantId, data);
  }

  @autobind
  handleUpload(data) {
    const merchantId = get(this.props.application, 'merchant.id');

    if (!merchantId) {
      console.error('No merchantId');

      return;
    }

    return this.props.upload(merchantId, data);
  }

  get form(): HTMLElement {
    const { feedSubmitInProgress, feedSubmitFailed } = this.props;

    return (
      <FeedForm
        className={styles.form}
        form="feed"
        fields={feedFields}
        initialValues={initialValues}
        onSubmit={this.submit}
        inProgress={feedSubmitInProgress}
        failed={feedSubmitFailed}
      />
    );
  }

  get upload(): HTMLElement {
    const { feedUploadInProgress, feedUploadFailed } = this.props;

    return (
      <UploadForm
        className={styles.form}
        form="upload"
        fields={uploadFields}
        onSubmit={this.handleUpload}
        inProgress={feedUploadInProgress}
        failed={feedUploadFailed}
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
        <div className={styles.forms}>
          {this.form}
          <div className={styles.or}>Or</div>
          {this.upload}
        </div>
      </div>
    );
  }
}

const mapState = state => ({
  application: getApplication(state),
  applicationFetched: getApplicationFetched(state),
  applicationFetchFailed: getApplicationFetchFailed(state),
  feedSubmitInProgress: getFeedSubmitInProgress(state),
  feedSubmitFailed: getFeedSubmitFailed(state),
  feedUploadInProgress: getFeedUploadInProgress(state),
  feedUploadFailed: getFeedUploadFailed(state),
});

export default connect(mapState, { fetchApplication, submit, upload, clearErrors, replace })(FeedPage);
