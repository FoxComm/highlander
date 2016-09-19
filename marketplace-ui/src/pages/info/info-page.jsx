/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';
import ThanksOrNot from '../../components/thanks-or-not/thanks-or-not';
import Loader from '../../components/loader/loader';

import { getInfoInProgress, getInfoFailed, getInfoDone } from '../../core/modules';
import { submit } from '../../core/modules/merchant-info';
import { fields } from '../../forms/info/info-fields';

import styles from './info-page.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  submit: Function;
  inProgress: boolean;
  done: boolean;
  failed: boolean;
}

class MerchantInfoPage extends Component {
  props: Props;

  get loader(): HTMLElement {
    if (!this.props.done) {
      return;
    }

    const message = <span>You're being redirected to admin page now</span>;

    return (
      <ThanksOrNot className={styles.thanksOrNot} title="You're done!" message={message}>
        <Loader />
      </ThanksOrNot>
    );
  }

  get form(): HTMLElement {
    if (this.props.done) {
      return;
    }

    const { submit, inProgress, failed } = this.props;

    return (
      <Form
        form="info"
        fields={fields}
        onSubmit={submit}
        inProgress={inProgress}
        failed={failed}
      />
    );
  }

  render(): HTMLElement {
    return (
      <div className={styles.info}>
        <Header
          title="We need additional business details."
          legend={'This additional information will help us ensure that we are able to' +
                  'add your business to our system and successfully send you payments.'}
        />
        {this.loader}
        {this.form}
      </div>
    );
  }
}

const mapState = state => ({
  inProgress: getInfoInProgress(state),
  done: getInfoDone(state),
  failed: getInfoFailed(state),
});

export default connect(mapState, { submit })(MerchantInfoPage);
