import React, { Component, PropTypes } from 'react';
import _ from 'lodash';

import * as adminSearchActions from '../../modules/orders/admin-search';

import wrapModal from '../modal/wrapper';
import ContentBox from '../content-box/content-box';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import ShareSearchInput from './share-search-input';

@wrapModal
export default class ShareSearch extends Component {
  constructor(props, ...args) {
    super(props, ...args);
  }

  static propTypes = {
    closeAction: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired
  };

  get closeAction() {
    return <a onClick={this.props.closeAction}>&times;</a>;
  }

  get title() {
    return <span>Share Search: <strong>{this.props.title}</strong></span>;
  }

  render() {
    return (
      <div className="fc-share-search">
        <div className="fc-modal-container">
          <ContentBox title={this.title} actionBlock={this.closeAction}>
            <div className="fc-share-search__search-form">
              <Form onSubmit={() => console.log('submit')}>
                <FormField label="Invite Users">
                  <ShareSearchInput actions={adminSearchActions} />
                </FormField>
                <FormField className="fc-share-search__submit-share">
                  <input className="fc-btn fc-btn-primary" type="submit" value="Share" />
                </FormField>
              </Form>
            </div>
          </ContentBox>
        </div>
      </div>
    );
  }
}
