/** Libs */
import { autobind } from 'core-decorators';
import React, { Component, PropTypes } from 'react';

/** Redux */

/** Component */
import wrapModal from '../modal/wrapper';
import ContentBox from '../content-box/content-box';
import Form from '../forms/form';
import FormField from '../forms/formfield';

@wrapModal
export default class ShareSearch extends Component {
  static propTypes = {
    closeAction: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired
  };

  state = {
    selectedUser: null
  };

  @autobind
  onItemSelected(item) {
    this.setState({
      selectedUser: item
    });
  }

  get closeAction() {
    return (
      <a className='fc-modal-close' onClick={this.props.closeAction}>
        <i className='icon-close'></i>
      </a>
    );
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
                </FormField>
                <FormField className="fc-share-search__submit-share">
                  <input className="fc-btn fc-btn-primary" type="submit" value="Share"
                         disabled={!this.state.selectedUser}/>
                </FormField>
              </Form>
            </div>
          </ContentBox>
        </div>
      </div>
    );
  }
}
