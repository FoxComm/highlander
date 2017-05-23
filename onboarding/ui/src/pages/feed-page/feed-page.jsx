/* @flow */

import { get, isEmpty } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import FeedForm from '../../forms/feed/feed-form';
import UploadForm from '../../forms/feed/upload-form';
import ThanksOrNot from '../../components/thanks-or-not/thanks-or-not';
import Loader from '../../components/loader/loader';

import {
  getApplication,
  getFeed,
  getFeedSubmitInProgress,
  getFeedSubmitFailed,
  getFeedUploadInProgress,
  getFeedUploadFailed,
} from '../../core/modules';
import { submit, upload } from '../../core/modules/products-feed';

import { fields as feedFields, initialValues } from '../../forms/feed/feed-fields';
import { fields as uploadFields } from '../../forms/feed/upload-fields';

import styles from './feed-page.css';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';
import type { Feed } from '../../core/modules/products-feed';

type Props = {
  params: Object;
  feed: Feed;
  application: Application;
  feedSubmitInProgress: boolean;
  feedSubmitFailed: boolean;
  feedUploadInProgress: boolean;
  feedUploadFailed: boolean;
  submit: (data: Object) => Promise<*>;
  upload: (data: Object) => Promise<*>;
  replace: (path: string) => void;
}

const TIMEOUT_REDIRECT = 3000;

class FeedPage extends Component {
  props: Props;

  componentWillReceiveProps(nextProps: Props) {
    if (!isEmpty(nextProps.feed)) {
      this.handleInfoSucceeded();
      // this.props.replace(`/application/${this.props.params.ref}/shipping`);
    }
  }

  handleInfoSucceeded() {
    if (!window) {
      return;
    }

    setTimeout(
      () => window.location.replace(window.__ASHES_URL__),
      TIMEOUT_REDIRECT
    );
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

  get loader(): HTMLElement {
    if (isEmpty(this.props.feed)) {
      return;
    }

    return (
      <ThanksOrNot
        className={styles.thanksOrNot}
        title="You're done!"
        message="You're being redirected to admin page now"
      >
        <Loader />
      </ThanksOrNot>
    );
  }

  get forms(): HTMLElement {
    if (!isEmpty(this.props.feed)) {
      return;
    }

    return (
      <div className={styles.forms}>
        {this.form}
        <div className={styles.or}>Or</div>
        {this.upload}
      </div>
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
        {this.loader}
        {this.forms}
      </div>
    );
  }
}

const mapState = state => ({
  feed: getFeed(state),
  application: getApplication(state),
  feedSubmitInProgress: getFeedSubmitInProgress(state),
  feedSubmitFailed: getFeedSubmitFailed(state),
  feedUploadInProgress: getFeedUploadInProgress(state),
  feedUploadFailed: getFeedUploadFailed(state),
});

export default connect(mapState, { submit, upload, replace })(FeedPage);
