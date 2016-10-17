/* @flow */

import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { replace } from 'react-router-redux';

import Header from '../../components/header/header';
import Form from '../../components/form/form';

import { getApplication, getInfo, getInfoSubmitInProgress, getInfoSubmitFailed } from '../../core/modules';
import { submit } from '../../core/modules/merchant-info';
import { fields } from '../../forms/info/info-fields';

import styles from './info-page.css';

import type { HTMLElement } from '../../core/types';
import type { Application } from '../../core/modules/merchant-application';
import type { Info } from '../../core/modules/merchant-info';

type Props = {
  application: Application;
  info: Info;
  submit: Function;
  inProgress: boolean;
  failed: boolean;
  params: { ref: string };
  replace: (path: string) => void;
}

class MerchantInfoPage extends Component {
  props: Props;

  componentWillReceiveProps(nextProps: Props): void {
    if (nextProps.info.id) {
      this.props.replace(`/application/${this.props.params.ref}/shipping`);
    }
  }

  @autobind
  submit(data) {
    const merchantId = get(this.props.application, 'merchant.id');

    if (!merchantId) {
      return;
    }

    return this.props.submit(merchantId, data);
  }

  get form(): HTMLElement {
    if (this.props.info.id) {
      return;
    }

    const { inProgress, failed } = this.props;

    return (
      <Form
        form="info"
        fields={fields}
        onSubmit={this.submit}
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
  application: getApplication(state),
  info: getInfo(state),
  inProgress: getInfoSubmitInProgress(state),
  failed: getInfoSubmitFailed(state),
});

export default connect(mapState, { submit, replace })(MerchantInfoPage);
